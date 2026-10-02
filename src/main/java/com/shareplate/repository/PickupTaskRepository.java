package com.shareplate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shareplate.entity.PickupTask;

public interface PickupTaskRepository extends JpaRepository<PickupTask, Long> {

	List<PickupTask> findByVolunteerId(Long volunteerId);

	List<PickupTask> findByListingId(Long listingId);

	Optional<PickupTask> findFirstByListingId(Long listingId);
}