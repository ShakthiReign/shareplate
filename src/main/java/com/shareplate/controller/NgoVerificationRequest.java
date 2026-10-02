package com.shareplate.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class NgoVerificationRequest {

	@NotBlank(message = "Darpan ID or Registration Number is required")
	@Size(min = 5, max = 64, message = "Registration number must be between 5 and 64 characters")
	private String darpanId;

	@NotBlank(message = "Official contact phone number is required")
	@Pattern(regexp = "^[0-9]{10}$", message = "Please enter a valid 10-digit mobile number")
	private String phone;

	public NgoVerificationRequest() {
	}

	public String getDarpanId() {
		return darpanId;
	}

	public void setDarpanId(String darpanId) {
		this.darpanId = darpanId;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
}