package com.shareplate.dto;

import com.shareplate.controller.UserResponse;
import com.shareplate.entity.Role;

public class AuthResponse {

	private UserResponse user;
	private String token;

	public AuthResponse() {
	}

	public AuthResponse(Long id, String name, String email, Role role, boolean verified, boolean ngoVerified,
			String token) {
		this.user = new UserResponse(id, name, email, role, verified, ngoVerified);
		this.token = token;
	}

	public UserResponse getUser() {
		return user;
	}

	public void setUser(UserResponse user) {
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}