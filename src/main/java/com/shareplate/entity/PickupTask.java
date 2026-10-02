package com.shareplate.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pickup_tasks")
public class PickupTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long listingId;

	private Long volunteerId;

	/*
	 * PICKUP CODE
	 *
	 * The donor receives the actual 6-digit pickup code and gives it physically to
	 * the volunteer.
	 *
	 * pickupCodeEncrypted: Encrypted copy of the actual code.
	 *
	 * pickupCodeHash: BCrypt hash used to verify the code entered by the volunteer.
	 *
	 * We keep BOTH because BCrypt is one-way, so the original code cannot be
	 * recovered from the hash.
	 */
	private String pickupCodeEncrypted;

	private String pickupCodeHash;

	/*
	 * DELIVERY CODE
	 *
	 * The NGO receives the actual 6-digit delivery code and gives it physically to
	 * the volunteer.
	 *
	 * deliveryCodeEncrypted: Encrypted copy of the actual code.
	 *
	 * deliveryCodeHash: BCrypt hash used for verification.
	 */
	private String deliveryCodeEncrypted;

	private String deliveryCodeHash;

	@Enumerated(EnumType.STRING)
	private TaskStatus status = TaskStatus.ASSIGNED;

	private LocalDateTime collectedAt;

	private LocalDateTime deliveredAt;

	public PickupTask() {
	}

	public Long getId() {
		return id;
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

	public String getPickupCodeEncrypted() {
		return pickupCodeEncrypted;
	}

	public void setPickupCodeEncrypted(String pickupCodeEncrypted) {
		this.pickupCodeEncrypted = pickupCodeEncrypted;
	}

	public String getPickupCodeHash() {
		return pickupCodeHash;
	}

	public void setPickupCodeHash(String pickupCodeHash) {
		this.pickupCodeHash = pickupCodeHash;
	}

	public String getDeliveryCodeEncrypted() {
		return deliveryCodeEncrypted;
	}

	public void setDeliveryCodeEncrypted(String deliveryCodeEncrypted) {
		this.deliveryCodeEncrypted = deliveryCodeEncrypted;
	}

	public String getDeliveryCodeHash() {
		return deliveryCodeHash;
	}

	public void setDeliveryCodeHash(String deliveryCodeHash) {
		this.deliveryCodeHash = deliveryCodeHash;
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