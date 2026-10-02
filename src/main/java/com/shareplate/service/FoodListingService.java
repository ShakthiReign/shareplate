package com.shareplate.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shareplate.entity.FoodListing;
import com.shareplate.entity.ListingStatus;
import com.shareplate.entity.Role;
import com.shareplate.entity.User;
import com.shareplate.repository.FoodListingRepository;
import com.shareplate.repository.UserRepository;

@Service
public class FoodListingService {

	private static final long MIN_PICKUP_MINUTES = 30;
	private static final long MAX_PICKUP_DAYS = 7;

	private final FoodListingRepository repo;
	private final UserRepository userRepo;

	public FoodListingService(FoodListingRepository repo, UserRepository userRepo) {
		this.repo = repo;
		this.userRepo = userRepo;
	}

	/*
	 * Create a new food listing.
	 *
	 * The controller has already replaced donorId with the authenticated user's ID.
	 *
	 * Pickup deadline rules: 1. A deadline is mandatory. 2. It cannot be in the
	 * past. 3. It must provide at least 30 minutes for the rescue process. 4. It
	 * cannot be more than 7 days from now.
	 */
	@Transactional
	public FoodListing create(FoodListing listing) {

		if (listing.getDonorId() == null) {
			throw new IllegalArgumentException("Donor ID is required");
		}

		validatePickupDeadline(listing.getPickupDeadline());

		listing.setStatus(ListingStatus.AVAILABLE);
		listing.setClaimedByNgoId(null);

		return repo.save(listing);
	}

	/*
	 * Validate whether the pickup deadline is realistic.
	 */
	private void validatePickupDeadline(LocalDateTime pickupDeadline) {

		if (pickupDeadline == null) {
			throw new IllegalArgumentException("Pickup deadline is required");
		}

		LocalDateTime now = LocalDateTime.now();

		if (pickupDeadline.isBefore(now)) {
			throw new IllegalArgumentException("Pickup deadline cannot be in the past");
		}

		Duration timeUntilPickup = Duration.between(now, pickupDeadline);

		if (timeUntilPickup.toMinutes() < MIN_PICKUP_MINUTES) {
			throw new IllegalArgumentException("Pickup deadline must be at least 30 minutes from now");
		}

		LocalDateTime maximumDeadline = now.plusDays(MAX_PICKUP_DAYS);

		if (pickupDeadline.isAfter(maximumDeadline)) {
			throw new IllegalArgumentException("Pickup deadline cannot be more than 7 days from now");
		}
	}

	/*
	 * Return all listings.
	 */
	@Transactional(readOnly = true)
	public List<FoodListing> all() {
		return repo.findAll();
	}

	/*
	 * Return only currently available listings.
	 */
	@Transactional(readOnly = true)
	public List<FoodListing> available() {
		return repo.findByStatus(ListingStatus.AVAILABLE);
	}

	/*
	 * Get one listing.
	 */
	@Transactional(readOnly = true)
	public FoodListing get(Long id) {
		return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Listing not found"));
	}

	/*
	 * NGO claims a listing.
	 *
	 * Requires the claiming organization to be an approved, verified NGO.
	 */
	@Transactional
	public FoodListing claim(Long id, Long ngoId) {

		if (ngoId == null) {
			throw new IllegalArgumentException("NGO authentication is required");
		}

		User ngo = userRepo.findById(ngoId).orElseThrow(() -> new IllegalArgumentException("User not found"));

		if (ngo.getRole() != Role.NGO) {
			throw new IllegalArgumentException("Only NGO organizations can claim food listings");
		}

		if (!ngo.isNgoVerified()) {
			throw new IllegalStateException(
					"NGO legal verification (Darpan ID / Reg No) is required before claiming food listings");
		}

		FoodListing listing = get(id);

		if (listing.getStatus() != ListingStatus.AVAILABLE) {
			throw new IllegalStateException("Listing is not available");
		}

		if (listing.getPickupDeadline() == null) {
			throw new IllegalStateException("Listing has no pickup deadline");
		}

		if (!listing.getPickupDeadline().isAfter(LocalDateTime.now())) {
			listing.setStatus(ListingStatus.EXPIRED);
			repo.save(listing);
			throw new IllegalStateException("Pickup deadline has passed. This listing has expired.");
		}

		listing.setClaimedByNgoId(ngoId);
		listing.setStatus(ListingStatus.CLAIMED);

		return repo.save(listing);
	}

	/*
	 * Status changes controlled by the pickup lifecycle.
	 */
	@Transactional
	public FoodListing status(Long id, ListingStatus newStatus) {

		FoodListing listing = get(id);
		ListingStatus currentStatus = listing.getStatus();

		if (newStatus == null) {
			throw new IllegalArgumentException("Status is required");
		}

		if (currentStatus == null) {
			throw new IllegalStateException("Listing has no current status");
		}

		if (currentStatus == newStatus) {
			return listing;
		}

		if (currentStatus == ListingStatus.AVAILABLE) {
			if (newStatus == ListingStatus.EXPIRED) {
				if (listing.getPickupDeadline() == null) {
					throw new IllegalStateException("Listing has no pickup deadline");
				}

				if (listing.getPickupDeadline().isAfter(LocalDateTime.now())) {
					throw new IllegalStateException("Listing cannot expire before its pickup deadline");
				}

				listing.setStatus(ListingStatus.EXPIRED);
				return repo.save(listing);
			}

			throw new IllegalStateException("Available listings must be claimed before their status can change");
		}

		if (currentStatus == ListingStatus.CLAIMED) {
			throw new IllegalStateException("Claimed listing status is controlled by the pickup workflow");
		}

		if (currentStatus == ListingStatus.COLLECTED) {
			throw new IllegalStateException("Collected listing status is controlled by the delivery workflow");
		}

		if (currentStatus == ListingStatus.DELIVERED) {
			throw new IllegalStateException("Delivered listings cannot be changed");
		}

		if (currentStatus == ListingStatus.EXPIRED) {
			throw new IllegalStateException("Expired listings cannot be changed");
		}

		throw new IllegalStateException("Invalid listing status transition");
	}
}