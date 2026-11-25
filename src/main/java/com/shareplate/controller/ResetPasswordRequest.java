package com.shareplate.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

	@NotBlank(message = "Email is required")
	private String email;

	@NotBlank(message = "6-digit OTP is required")
	@Size(min = 6, max = 6, message = "OTP must be 6 digits")
	@Pattern(regexp = "\\d{6}", message = "OTP must contain exactly 6 digits")
	private String token;

	@NotBlank(message = "New password is required")
	@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
	private String newPassword;

	@NotBlank(message = "Password confirmation is required")
	private String confirmPassword;

	public ResetPasswordRequest() {
	}

	public String getEmail() { return email; }

	public void setEmail(String email) { this.email = email; }

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}
}