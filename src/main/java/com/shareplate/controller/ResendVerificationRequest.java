package com.shareplate.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ResendVerificationRequest {

	@NotBlank(message = "Email is required")
	@Email(message = "Enter a valid email address")
	private String email;

	public ResendVerificationRequest() {
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}