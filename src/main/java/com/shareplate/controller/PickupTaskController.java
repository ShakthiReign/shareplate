package com.shareplate.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shareplate.dto.PickupTaskResponse;
import com.shareplate.entity.FoodListing;
import com.shareplate.entity.PickupTask;
import com.shareplate.entity.User;
import com.shareplate.repository.FoodListingRepository;
import com.shareplate.repository.PickupTaskRepository;
import com.shareplate.repository.UserRepository;
import com.shareplate.service.PickupTaskService;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = {"http://localhost:5173", "https://shareplate-green.vercel.app"})
public class PickupTaskController {

	private final PickupTaskService service;

	@Autowired
	private FoodListingRepository foodListingRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PickupTaskRepository pickupTaskRepository;

	public PickupTaskController(PickupTaskService service) {
		this.service = service;
	}

	@PostMapping
	public PickupTaskResponse assign(@RequestParam Long listingId, @RequestParam Long volunteerId,
			Authentication authentication) {

		requireRole(authentication, "NGO");
		Long ngoId = getUserId(authentication);

		return toResponse(service.assign(listingId, volunteerId, ngoId));
	}

	@GetMapping
	public List<PickupTaskResponse> all(Authentication authentication) {

		String role = getRole(authentication);
		Long userId = getUserId(authentication);

		if ("DONOR".equals(role)) {
			return service.byDonor(userId).stream().map(this::toResponse).toList();
		}
		if ("NGO".equals(role)) {
			return service.byNgo(userId).stream().map(this::toResponse).toList();
		}
		if ("VOLUNTEER".equals(role)) {
			return service.byVolunteer(userId).stream().map(this::toResponse).toList();
		}

		throw new IllegalArgumentException("Unsupported user role");
	}

	@GetMapping("/volunteer/{ignoredVolunteerId}")
	public List<PickupTaskResponse> byVolunteer(@PathVariable Long ignoredVolunteerId, Authentication authentication) {

		requireRole(authentication, "VOLUNTEER");
		Long volunteerId = getUserId(authentication);
		return service.byVolunteer(volunteerId).stream().map(this::toResponse).toList();
	}

	@GetMapping("/{id}/pickup-code")
	public Map<String, String> getPickupCode(@PathVariable Long id, Authentication authentication) {

		requireRole(authentication, "DONOR");
		Long donorId = getUserId(authentication);
		String code = service.getPickupCode(id, donorId);
		return Map.of("code", code);
	}

	@GetMapping("/{id}/delivery-code")
	public Map<String, String> getDeliveryCode(@PathVariable Long id, Authentication authentication) {

		requireRole(authentication, "NGO");
		Long ngoId = getUserId(authentication);
		String code = service.getDeliveryCode(id, ngoId);
		return Map.of("code", code);
	}

	@PostMapping("/{id}/collect")
	public PickupTaskResponse collect(@PathVariable Long id, @RequestBody Map<String, String> body,
			Authentication authentication) {

		requireRole(authentication, "VOLUNTEER");
		Long volunteerId = getUserId(authentication);
		String code = body == null ? null : body.get("code");
		return toResponse(service.collect(id, volunteerId, code));
	}

	@PostMapping("/{id}/deliver")
	public PickupTaskResponse deliver(@PathVariable Long id, @RequestBody Map<String, String> body,
			Authentication authentication) {

		requireRole(authentication, "VOLUNTEER");
		Long volunteerId = getUserId(authentication);
		String code = body == null ? null : body.get("code");
		return toResponse(service.deliver(id, volunteerId, code));
	}

	// Live Volunteer Telemetry Endpoint (called by main.jsx line 172)
	@PostMapping("/{id}/location")
	public ResponseEntity<?> updateLocation(
			@PathVariable Long id,
			@RequestBody Map<String, Object> body,
			Authentication authentication) {

		requireRole(authentication, "VOLUNTEER");

		PickupTask task = pickupTaskRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Task not found"));

		if (body.containsKey("latitude") && body.get("latitude") != null) {
			task.setVolunteerLatitude(Double.parseDouble(body.get("latitude").toString()));
		}
		if (body.containsKey("longitude") && body.get("longitude") != null) {
			task.setVolunteerLongitude(Double.parseDouble(body.get("longitude").toString()));
		}

		pickupTaskRepository.save(task);
		return ResponseEntity.ok(Map.of("message", "Location updated successfully"));
	}

	private Long getUserId(Authentication authentication) {

		if (authentication == null || authentication.getPrincipal() == null) {
			throw new IllegalArgumentException("User is not authenticated");
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof Long userId) return userId;
		if (principal instanceof Number number) return number.longValue();

		try {
			return Long.parseLong(principal.toString());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid authenticated user");
		}
	}

	private String getRole(Authentication authentication) {

		if (authentication == null) {
			throw new IllegalArgumentException("User is not authenticated");
		}

		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String name = authority.getAuthority();
			if (name != null) {
				if (name.startsWith("ROLE_")) {
					return name.substring("ROLE_".length()).trim();
				}
				return name.trim();
			}
		}

		throw new IllegalArgumentException("User role not found");
	}

	private void requireRole(Authentication authentication, String expectedRole) {

		String actualRole = getRole(authentication);
		String cleanExpected = expectedRole.startsWith("ROLE_") ? expectedRole.substring(5) : expectedRole;
		String cleanActual = actualRole.startsWith("ROLE_") ? actualRole.substring(5) : actualRole;

		if (!cleanExpected.equalsIgnoreCase(cleanActual)) {
			throw new IllegalArgumentException("Only " + cleanExpected + " users can perform this action");
		}
	}
	private PickupTaskResponse toResponse(PickupTask task) {

		PickupTaskResponse resp = new PickupTaskResponse(
				task.getId(), task.getListingId(), task.getVolunteerId(), task.getStatus(),
				task.getCollectedAt(), task.getDeliveredAt());

		// Populate Volunteer Contact Info
		if (task.getVolunteerId() != null) {
			userRepository.findById(task.getVolunteerId()).ifPresent(v -> {
				resp.setVolunteerName(v.getName());
				resp.setVolunteerPhone(v.getPhone());
				resp.setVolunteerLatitude(v.getLatitude());
				resp.setVolunteerLongitude(v.getLongitude());
			});
		}

		// Populate Listing Location and Donor Phone
		if (task.getListingId() != null) {
			foodListingRepository.findById(task.getListingId()).ifPresent(l -> {
				resp.setPickupAddress(l.getLocation());
				resp.setPickupLatitude(l.getLatitude());
				resp.setPickupLongitude(l.getLongitude());
				resp.setDonorPhone(l.getDonorPhone());

				// Populate NGO Phone
				if (l.getClaimedByNgoId() != null) {
					userRepository.findById(l.getClaimedByNgoId()).ifPresent(ngo -> {
						resp.setNgoPhone(ngo.getPhone());
					});
				}
			});
		}

		return resp;
	}
}