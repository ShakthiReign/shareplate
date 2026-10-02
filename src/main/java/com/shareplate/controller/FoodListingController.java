package com.shareplate.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shareplate.entity.FoodListing;
import com.shareplate.entity.ListingStatus;
import com.shareplate.service.FoodListingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/listings")
@CrossOrigin(origins = "http://localhost:5173")
public class FoodListingController {

	private final FoodListingService service;

	public FoodListingController(FoodListingService service) {
		this.service = service;
	}

	/*
	 * The authenticated user's ID comes from the JWT. We do NOT trust donorId/ngoId
	 * supplied by the frontend.
	 */
	private Long getAuthenticatedUserId(Authentication authentication) {
		if (authentication == null || authentication.getPrincipal() == null) {
			throw new IllegalArgumentException("Authentication required");
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

	private String getAuthenticatedRole(Authentication authentication) {
		if (authentication == null) {
			throw new IllegalArgumentException("Authentication required");
		}

		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String name = authority.getAuthority();

			if (name != null && name.startsWith("ROLE_")) {
				return name.substring("ROLE_".length());
			}
		}

		throw new IllegalArgumentException("User role not found");
	}

	/*
	 * DONOR - CREATE LISTING
	 *
	 * The frontend may still send donorId because the existing frontend currently
	 * does so, but we deliberately overwrite it with the authenticated user's ID.
	 */
	@PostMapping
	public FoodListing create(@Valid @RequestBody FoodListing listing, Authentication authentication) {

		String role = getAuthenticatedRole(authentication);

		if (!"DONOR".equals(role)) {
			throw new IllegalArgumentException("Only a donor can create a food listing");
		}

		Long authenticatedUserId = getAuthenticatedUserId(authentication);

		listing.setDonorId(authenticatedUserId);

		return service.create(listing);
	}

	/*
	 * AUTHENTICATED USERS - VIEW ALL LISTINGS
	 */
	@GetMapping
	public List<FoodListing> all() {
		return service.all();
	}

	/*
	 * AUTHENTICATED USERS - VIEW AVAILABLE LISTINGS
	 */
	@GetMapping("/available")
	public List<FoodListing> available() {
		return service.available();
	}

	/*
	 * AUTHENTICATED USERS - VIEW ONE LISTING
	 */
	@GetMapping("/{id}")
	public FoodListing get(@PathVariable Long id) {
		return service.get(id);
	}

	/*
	 * NGO - CLAIM LISTING
	 *
	 * We keep ngoId in the URL temporarily so the current frontend does not break.
	 *
	 * IMPORTANT: The supplied ngoId is completely ignored. The authenticated JWT
	 * user ID is used instead.
	 */
	@PostMapping("/{id}/claim/{ignoredNgoId}")
	public FoodListing claim(@PathVariable Long id, @PathVariable Long ignoredNgoId, Authentication authentication) {

		String role = getAuthenticatedRole(authentication);

		if (!"NGO".equals(role)) {
			throw new IllegalArgumentException("Only an NGO can claim a food listing");
		}

		Long authenticatedUserId = getAuthenticatedUserId(authentication);

		return service.claim(id, authenticatedUserId);
	}

	/*
	 * STATUS UPDATE
	 *
	 * This endpoint will be audited further in FoodListingService. For now we keep
	 * the existing service signature so we don't accidentally break the current V1
	 * workflow.
	 */
	@PatchMapping("/{id}/status/{status}")
	public FoodListing status(@PathVariable Long id, @PathVariable ListingStatus status,
			Authentication authentication) {

		getAuthenticatedUserId(authentication);

		return service.status(id, status);
	}
}