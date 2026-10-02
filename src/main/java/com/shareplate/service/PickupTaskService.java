package com.shareplate.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shareplate.entity.FoodListing;
import com.shareplate.entity.PickupTask;
import com.shareplate.entity.Role;
import com.shareplate.entity.TaskStatus;
import com.shareplate.entity.User;
import com.shareplate.repository.FoodListingRepository;
import com.shareplate.repository.PickupTaskRepository;
import com.shareplate.repository.UserRepository;

@Service
public class PickupTaskService {

	private final PickupTaskRepository repo;
	private final FoodListingRepository foodListingRepository;
	private final UserRepository userRepository;
	private final VerificationCodeService verificationCodeService;

	public PickupTaskService(PickupTaskRepository repo, FoodListingRepository foodListingRepository,
			UserRepository userRepository, VerificationCodeService verificationCodeService) {

		this.repo = repo;
		this.foodListingRepository = foodListingRepository;
		this.userRepository = userRepository;
		this.verificationCodeService = verificationCodeService;
	}

	// --------------------------------------------------
	// ASSIGN VOLUNTEER
	// NGO -> VOLUNTEER
	// --------------------------------------------------

	@Transactional
	public PickupTask assign(Long listingId, Long volunteerId, Long ngoId) {

		if (listingId == null) {
			throw new IllegalArgumentException("Listing ID is required");
		}

		if (volunteerId == null) {
			throw new IllegalArgumentException("Volunteer ID is required");
		}

		if (ngoId == null) {
			throw new IllegalArgumentException("NGO ID is required");
		}

		FoodListing listing = getListing(listingId);

		// --------------------------------------------------
		// VERIFY NGO CLAIM
		// --------------------------------------------------

		if (listing.getClaimedByNgoId() == null) {
			throw new IllegalArgumentException("This listing has not been claimed by an NGO");
		}

		if (!listing.getClaimedByNgoId().equals(ngoId)) {
			throw new IllegalArgumentException("You are not authorized to assign a volunteer to this listing");
		}

		// --------------------------------------------------
		// VERIFY LISTING STATE
		// --------------------------------------------------

		if (listing.getStatus() != com.shareplate.entity.ListingStatus.CLAIMED) {
			throw new IllegalArgumentException("A volunteer can only be assigned to a claimed listing");
		}

		// --------------------------------------------------
		// VERIFY VOLUNTEER
		// --------------------------------------------------

		User volunteer = userRepository.findById(volunteerId)
				.orElseThrow(() -> new IllegalArgumentException("Volunteer not found"));

		if (volunteer.getRole() != Role.VOLUNTEER) {
			throw new IllegalArgumentException("Selected user is not a volunteer");
		}

		// --------------------------------------------------
		// PREVENT DUPLICATE TASK
		// --------------------------------------------------

		if (repo.findFirstByListingId(listingId).isPresent()) {
			throw new IllegalArgumentException("A volunteer has already been assigned to this listing");
		}

		// --------------------------------------------------
		// CREATE TASK
		// --------------------------------------------------

		PickupTask task = new PickupTask();

		task.setListingId(listingId);
		task.setVolunteerId(volunteerId);

		String pickupCode = verificationCodeService.generateCode();

		String deliveryCode = verificationCodeService.generateCode();

		task.setPickupCodeEncrypted(verificationCodeService.encryptCode(pickupCode));

		task.setDeliveryCodeEncrypted(verificationCodeService.encryptCode(deliveryCode));

		task.setPickupCodeHash(verificationCodeService.hashCode(pickupCode));

		task.setDeliveryCodeHash(verificationCodeService.hashCode(deliveryCode));

		task.setStatus(TaskStatus.ASSIGNED);

		/*
		 * FoodListing remains CLAIMED.
		 *
		 * PickupTask becomes ASSIGNED.
		 */
		foodListingRepository.save(listing);

		return repo.save(task);
	}

	// --------------------------------------------------
	// GET TASKS FOR LOGGED-IN USER
	// --------------------------------------------------

	public List<PickupTask> all() {
		return repo.findAll();
	}

	// --------------------------------------------------
	// GET DONOR TASKS
	// --------------------------------------------------

	public List<PickupTask> byDonor(Long donorId) {

		if (donorId == null) {
			throw new IllegalArgumentException("Donor ID is required");
		}

		List<FoodListing> listings = foodListingRepository.findByDonorId(donorId);

		List<PickupTask> tasks = new ArrayList<>();

		for (FoodListing listing : listings) {

			PickupTask task = repo.findFirstByListingId(listing.getId()).orElse(null);

			if (task != null) {
				tasks.add(task);
			}
		}

		return tasks;
	}

	// --------------------------------------------------
	// GET NGO TASKS
	// --------------------------------------------------

	public List<PickupTask> byNgo(Long ngoId) {

		if (ngoId == null) {
			throw new IllegalArgumentException("NGO ID is required");
		}

		List<FoodListing> listings = foodListingRepository.findAll();

		List<PickupTask> tasks = new ArrayList<>();

		for (FoodListing listing : listings) {

			if (listing.getClaimedByNgoId() == null || !listing.getClaimedByNgoId().equals(ngoId)) {
				continue;
			}

			PickupTask task = repo.findFirstByListingId(listing.getId()).orElse(null);

			if (task != null) {
				tasks.add(task);
			}
		}

		return tasks;
	}

	// --------------------------------------------------
	// GET VOLUNTEER TASKS
	// --------------------------------------------------

	public List<PickupTask> byVolunteer(Long volunteerId) {

		if (volunteerId == null) {
			throw new IllegalArgumentException("Volunteer ID is required");
		}

		return repo.findByVolunteerId(volunteerId);
	}

	// --------------------------------------------------
	// GET PICKUP CODE
	// DONOR ONLY
	// --------------------------------------------------

	public String getPickupCode(Long taskId, Long donorId) {

		PickupTask task = get(taskId);

		FoodListing listing = getListing(task.getListingId());

		if (listing.getDonorId() == null || !listing.getDonorId().equals(donorId)) {

			throw new IllegalArgumentException("You are not authorized to view the pickup code");
		}

		return verificationCodeService.decryptCode(task.getPickupCodeEncrypted());
	}

	// --------------------------------------------------
	// GET DELIVERY CODE
	// NGO ONLY
	// --------------------------------------------------

	public String getDeliveryCode(Long taskId, Long ngoId) {

		PickupTask task = get(taskId);

		FoodListing listing = getListing(task.getListingId());

		if (listing.getClaimedByNgoId() == null || !listing.getClaimedByNgoId().equals(ngoId)) {

			throw new IllegalArgumentException("You are not authorized to view the delivery code");
		}

		return verificationCodeService.decryptCode(task.getDeliveryCodeEncrypted());
	}

	// --------------------------------------------------
	// VOLUNTEER - COLLECT
	//
	// ASSIGNED -> COLLECTED
	// --------------------------------------------------

	@Transactional
	public PickupTask collect(Long id, Long volunteerId, String code) {

		PickupTask task = get(id);

		if (task.getVolunteerId() == null || !task.getVolunteerId().equals(volunteerId)) {

			throw new IllegalArgumentException("You are not authorized to collect this food");
		}

		if (task.getStatus() != TaskStatus.ASSIGNED) {

			throw new IllegalArgumentException("Food has already been collected or this task is no longer active");
		}

		if (code == null || code.trim().isEmpty()) {

			throw new IllegalArgumentException("Pickup code is required");
		}

		boolean valid = verificationCodeService.matches(code.trim(), task.getPickupCodeHash());

		if (!valid) {

			throw new IllegalArgumentException("Invalid pickup code");
		}

		task.setStatus(TaskStatus.COLLECTED);
		task.setCollectedAt(LocalDateTime.now());

		FoodListing listing = getListing(task.getListingId());

		listing.setStatus(com.shareplate.entity.ListingStatus.COLLECTED);

		foodListingRepository.save(listing);

		return repo.save(task);
	}

	// --------------------------------------------------
	// VOLUNTEER - DELIVER
	//
	// COLLECTED -> DELIVERED
	// --------------------------------------------------

	@Transactional
	public PickupTask deliver(Long id, Long volunteerId, String code) {

		PickupTask task = get(id);

		if (task.getVolunteerId() == null || !task.getVolunteerId().equals(volunteerId)) {

			throw new IllegalArgumentException("You are not authorized to deliver this food");
		}

		if (task.getStatus() != TaskStatus.COLLECTED) {

			throw new IllegalArgumentException("Food must be collected before it can be delivered");
		}

		if (code == null || code.trim().isEmpty()) {

			throw new IllegalArgumentException("Delivery code is required");
		}

		boolean valid = verificationCodeService.matches(code.trim(), task.getDeliveryCodeHash());

		if (!valid) {

			throw new IllegalArgumentException("Invalid delivery code");
		}

		task.setStatus(TaskStatus.DELIVERED);
		task.setDeliveredAt(LocalDateTime.now());

		FoodListing listing = getListing(task.getListingId());

		listing.setStatus(com.shareplate.entity.ListingStatus.DELIVERED);

		foodListingRepository.save(listing);

		return repo.save(task);
	}

	// --------------------------------------------------
	// FIND TASK
	// --------------------------------------------------

	private PickupTask get(Long id) {

		if (id == null) {
			throw new IllegalArgumentException("Task ID is required");
		}

		return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found"));
	}

	// --------------------------------------------------
	// FIND FOOD LISTING
	// --------------------------------------------------

	private FoodListing getListing(Long listingId) {

		if (listingId == null) {
			throw new IllegalArgumentException("Food listing ID is required");
		}

		return foodListingRepository.findById(listingId)
				.orElseThrow(() -> new IllegalArgumentException("Food listing not found"));
	}
}