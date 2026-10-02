package com.shareplate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(name = "uk_users_email", columnNames = "email") })
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Column(nullable = false)
	private String name;

	@Email
	@NotBlank
	@Column(nullable = false, unique = true)
	private String email;

	@NotBlank
	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@NotNull
	@Column(nullable = false)
	private Role role;

	@Column(nullable = false)
	private boolean verified = false;

	// NGO Specific Legal Credentials
	@Column(name = "darpan_id", length = 64)
	private String darpanId;

	@Column(name = "phone", length = 20)
	private String phone;

	@Column(name = "ngo_verified", nullable = false)
	private boolean ngoVerified = false;

	public User() {
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
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

	public boolean isNgoVerified() {
		return ngoVerified;
	}

	public void setNgoVerified(boolean ngoVerified) {
		this.ngoVerified = ngoVerified;
	}
}