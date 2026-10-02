package com.shareplate.dto;

import java.time.LocalDateTime;

import com.shareplate.entity.TaskStatus;

public class PickupTaskResponse {

	private Long id;
	private Long listingId;
	private Long volunteerId;
	private TaskStatus status;
	private LocalDateTime collectedAt;
	private LocalDateTime deliveredAt;

	public PickupTaskResponse() {
	}

	public PickupTaskResponse(Long id, Long listingId, Long volunteerId, TaskStatus status, LocalDateTime collectedAt,
			LocalDateTime deliveredAt) {

		this.id = id;
		this.listingId = listingId;
		this.volunteerId = volunteerId;
		this.status = status;
		this.collectedAt = collectedAt;
		this.deliveredAt = deliveredAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getListingId() {
		return listingId;
	}

	public void setListingId(Long listingId) {
		this.listingId = listingId;
	}

	public Long getVolunteerId() {
		return volunteerId;
	}

	public void setVolunteerId(Long volunteerId) {
		this.volunteerId = volunteerId;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public LocalDateTime getCollectedAt() {
		return collectedAt;
	}

	public void setCollectedAt(LocalDateTime collectedAt) {
		this.collectedAt = collectedAt;
	}

	public LocalDateTime getDeliveredAt() {
		return deliveredAt;
	}

	public void setDeliveredAt(LocalDateTime deliveredAt) {
		this.deliveredAt = deliveredAt;
	}
}