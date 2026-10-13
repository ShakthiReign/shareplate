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

	// Fields required by the UI for Volunteer & NGO coordination
	private String volunteerName;
	private String volunteerPhone;
	private String donorPhone;
	private String ngoPhone;
	private Double pickupLatitude;
	private Double pickupLongitude;
	private String pickupAddress;
	private Double volunteerLatitude;
	private Double volunteerLongitude;

	public PickupTaskResponse() {
	}

	public PickupTaskResponse(Long id, Long listingId, Long volunteerId, TaskStatus status,
			LocalDateTime collectedAt, LocalDateTime deliveredAt) {
		this.id = id;
		this.listingId = listingId;
		this.volunteerId = volunteerId;
		this.status = status;
		this.collectedAt = collectedAt;
		this.deliveredAt = deliveredAt;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getListingId() { return listingId; }
	public void setListingId(Long listingId) { this.listingId = listingId; }

	public Long getVolunteerId() { return volunteerId; }
	public void setVolunteerId(Long volunteerId) { this.volunteerId = volunteerId; }

	public TaskStatus getStatus() { return status; }
	public void setStatus(TaskStatus status) { this.status = status; }

	public LocalDateTime getCollectedAt() { return collectedAt; }
	public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }

	public LocalDateTime getDeliveredAt() { return deliveredAt; }
	public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

	public String getVolunteerName() { return volunteerName; }
	public void setVolunteerName(String volunteerName) { this.volunteerName = volunteerName; }

	public String getVolunteerPhone() { return volunteerPhone; }
	public void setVolunteerPhone(String volunteerPhone) { this.volunteerPhone = volunteerPhone; }

	public String getDonorPhone() { return donorPhone; }
	public void setDonorPhone(String donorPhone) { this.donorPhone = donorPhone; }

	public String getNgoPhone() { return ngoPhone; }
	public void setNgoPhone(String ngoPhone) { this.ngoPhone = ngoPhone; }

	public Double getPickupLatitude() { return pickupLatitude; }
	public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }

	public Double getPickupLongitude() { return pickupLongitude; }
	public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }

	public String getPickupAddress() { return pickupAddress; }
	public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

	public Double getVolunteerLatitude() { return volunteerLatitude; }
	public void setVolunteerLatitude(Double volunteerLatitude) { this.volunteerLatitude = volunteerLatitude; }

	public Double getVolunteerLongitude() { return volunteerLongitude; }
	public void setVolunteerLongitude(Double volunteerLongitude) { this.volunteerLongitude = volunteerLongitude; }
}