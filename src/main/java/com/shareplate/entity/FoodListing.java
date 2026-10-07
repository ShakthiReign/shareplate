package com.shareplate.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "food_listings")
public class FoodListing {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotBlank
	private String foodName;
	private String description;
	@Min(1)
	private int quantity;
	@NotBlank
	private String location;
	@NotNull
	private LocalDateTime pickupDeadline;
	private String safetyDetails;
	private Long donorId;
	private Long claimedByNgoId;
	
	// Coordinates & Contact fields
	private Double latitude;
	private Double longitude;
	private String donorPhone;

	@Enumerated(EnumType.STRING)
	private ListingStatus status = ListingStatus.AVAILABLE;

	public FoodListing() {
	}

	public Long getId() {
		return id;
	}

	public String getFoodName() {
		return foodName;
	}

	public void setFoodName(String v) {
		foodName = v;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String v) {
		description = v;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int v) {
		quantity = v;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String v) {
		location = v;
	}

	public LocalDateTime getPickupDeadline() {
		return pickupDeadline;
	}

	public void setPickupDeadline(LocalDateTime v) {
		pickupDeadline = v;
	}

	public String getSafetyDetails() {
		return safetyDetails;
	}

	public void setSafetyDetails(String v) {
		safetyDetails = v;
	}

	public Long getDonorId() {
		return donorId;
	}

	public void setDonorId(Long v) {
		donorId = v;
	}

	public Long getClaimedByNgoId() {
		return claimedByNgoId;
	}

	public void setClaimedByNgoId(Long v) {
		claimedByNgoId = v;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getDonorPhone() {
		return donorPhone;
	}

	public void setDonorPhone(String donorPhone) {
		this.donorPhone = donorPhone;
	}

	public ListingStatus getStatus() {
		return status;
	}

	public void setStatus(ListingStatus v) {
		status = v;
	}
}