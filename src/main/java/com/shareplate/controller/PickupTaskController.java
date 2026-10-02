package com.shareplate.controller;

import java.util.List;
import java.util.Map;

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
import com.shareplate.entity.PickupTask;
import com.shareplate.service.PickupTaskService;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = {"http://localhost:5173", "https://shareplate-green.vercel.app"})
public class PickupTaskController {

	private final PickupTaskService service;

	public PickupTaskController(PickupTaskService service) {
		this.service = service;
	}

	// --------------------------------------------------
	// NGO - ASSIGN VOLUNTEER
	// --------------------------------------------------

	@PostMapping
	public PickupTaskResponse assign(@RequestParam Long listingId, @RequestParam Long volunteerId,
			Authentication authentication) {

		requireRole(authentication, "NGO");

		Long ngoId = getUserId(authentication);

		return toResponse(service.assign(listingId, volunteerId, ngoId));
	}

	// --------------------------------------------------
	// GET TASKS FOR LOGGED-IN USER
	// --------------------------------------------------

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

	// --------------------------------------------------
	// VOLUNTEER - GET OWN TASKS
	// --------------------------------------------------

	@GetMapping("/volunteer/{ignoredVolunteerId}")
	public List<PickupTaskResponse> byVolunteer(@PathVariable Long ignoredVolunteerId, Authentication authentication) {

		requireRole(authentication, "VOLUNTEER");

		Long volunteerId = getUserId(authentication);

		/*
		 * The URL ID is intentionally ignored.
		 *
		 * The authenticated JWT ID is always used.
		 */
		return service.byVolunteer(volunteerId).stream().map(this::toResponse).toList();
	}

	// --------------------------------------------------
	// DONOR - GET PICKUP CODE
	// --------------------------------------------------

	@GetMapping("/{id}/pickup-code")
	public Map<String, String> getPickupCode(@PathVariable Long id, Authentication authentication) {

		requireRole(authentication, "DONOR");

		Long donorId = getUserId(authentication);

		String code = service.getPickupCode(id, donorId);

		return Map.of("code", code);
	}

	// --------------------------------------------------
	// NGO - GET DELIVERY CODE
	// --------------------------------------------------

	@GetMapping("/{id}/delivery-code")
	public Map<String, String> getDeliveryCode(@PathVariable Long id, Authentication authentication) {

		requireRole(authentication, "NGO");

		Long ngoId = getUserId(authentication);

		String code = service.getDeliveryCode(id, ngoId);

		return Map.of("code", code);
	}

	// --------------------------------------------------
	// VOLUNTEER - COLLECT
	// --------------------------------------------------

	@PostMapping("/{id}/collect")
	public PickupTaskResponse collect(@PathVariable Long id, @RequestBody Map<String, String> body,
			Authentication authentication) {

		requireRole(authentication, "VOLUNTEER");

		Long volunteerId = getUserId(authentication);

		String code = body == null ? null : body.get("code");

		return toResponse(service.collect(id, volunteerId, code));
	}

	// --------------------------------------------------
	// VOLUNTEER - DELIVER
	// --------------------------------------------------

	@PostMapping("/{id}/deliver")
	public PickupTaskResponse deliver(@PathVariable Long id, @RequestBody Map<String, String> body,
			Authentication authentication) {

		requireRole(authentication, "VOLUNTEER");

		Long volunteerId = getUserId(authentication);

		String code = body == null ? null : body.get("code");

		return toResponse(service.deliver(id, volunteerId, code));
	}

	// --------------------------------------------------
	// GET LOGGED-IN USER ID
	// --------------------------------------------------

	private Long getUserId(Authentication authentication) {

		if (authentication == null || authentication.getPrincipal() == null) {

			throw new IllegalArgumentException("User is not authenticated");
		}

		Object principal = authentication.getPrincipal();

		if (principal instanceof Long userId) {
			return userId;
		}

		if (principal instanceof Number number) {
			return number.longValue();
		}

		try {
			return Long.parseLong(principal.toString());
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid authenticated user");
		}
	}

	// --------------------------------------------------
	// GET LOGGED-IN USER ROLE
	// --------------------------------------------------

	private String getRole(Authentication authentication) {

		if (authentication == null) {
			throw new IllegalArgumentException("User is not authenticated");
		}

		for (GrantedAuthority authority : authentication.getAuthorities()) {

			String name = authority.getAuthority();

			if (name != null && name.startsWith("ROLE_")) {
				return name.substring("ROLE_".length());
			}
		}

		throw new IllegalArgumentException("User role not found");
	}

	// --------------------------------------------------
	// ROLE CHECK
	// --------------------------------------------------

	private void requireRole(Authentication authentication, String expectedRole) {

		String actualRole = getRole(authentication);

		if (!expectedRole.equals(actualRole)) {

			throw new IllegalArgumentException("Only " + expectedRole + " users can perform this action");
		}
	}

	// --------------------------------------------------
	// ENTITY -> DTO
	// --------------------------------------------------

	private PickupTaskResponse toResponse(PickupTask task) {

		return new PickupTaskResponse(task.getId(), task.getListingId(), task.getVolunteerId(), task.getStatus(),
				task.getCollectedAt(), task.getDeliveredAt());
	}
}
