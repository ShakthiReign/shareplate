package com.shareplate.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.shareplate.entity.User;
import com.shareplate.repository.UserRepository;
import com.shareplate.service.FoodListingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/listings")
@CrossOrigin(origins = {"http://localhost:5173", "https://shareplate-green.vercel.app"})
public class FoodListingController {

    private final FoodListingService service;

    @Autowired
    private UserRepository userRepository;

    public FoodListingController(FoodListingService service) {
        this.service = service;
    }

    private Long getAuthenticatedUserId(Authentication authentication, Long fallbackUserId) {
        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof Long userId) {
                return userId;
            }
            if (principal instanceof Number number) {
                return number.longValue();
            }
            try {
                return Long.parseLong(principal.toString());
            } catch (NumberFormatException ignored) {}
        }

        if (fallbackUserId != null) {
            return fallbackUserId;
        }

        throw new IllegalArgumentException("Authentication required: user ID missing");
    }

    private String getAuthenticatedRole(Authentication authentication, Long userId) {
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String name = authority.getAuthority();
                if (name != null && name.startsWith("ROLE_")) {
                    return name.substring("ROLE_".length());
                }
            }
        }

        if (userId != null) {
            return userRepository.findById(userId)
                    .map(User::getRole)
                    .map(Enum::name)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }

        throw new IllegalArgumentException("Authentication required: role missing");
    }

    /*
     * DONOR - CREATE LISTING
     */
    @PostMapping
    public FoodListing create(@Valid @RequestBody FoodListing listing, Authentication authentication) {
        Long effectiveUserId = getAuthenticatedUserId(authentication, listing.getDonorId());
        String role = getAuthenticatedRole(authentication, effectiveUserId);

        if (!"DONOR".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Only a donor can create a food listing");
        }

        listing.setDonorId(effectiveUserId);
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
     */
    @PostMapping("/{id}/claim/{ngoId}")
    public FoodListing claim(@PathVariable Long id, @PathVariable Long ngoId, Authentication authentication) {
        Long effectiveNgoId = getAuthenticatedUserId(authentication, ngoId);
        String role = getAuthenticatedRole(authentication, effectiveNgoId);

        if (!"NGO".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Only an NGO can claim a food listing");
        }

        return service.claim(id, effectiveNgoId);
    }

    /*
     * STATUS UPDATE
     */
    @PatchMapping("/{id}/status/{status}")
    public FoodListing status(@PathVariable Long id, @PathVariable ListingStatus status,
            Authentication authentication) {
        return service.status(id, status);
    }
}