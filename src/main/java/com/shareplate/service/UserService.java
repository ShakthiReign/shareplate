package com.shareplate.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shareplate.controller.LoginRequest;
import com.shareplate.controller.RegisterRequest;
import com.shareplate.dto.AuthResponse;
import com.shareplate.entity.Role;
import com.shareplate.entity.User;
import com.shareplate.repository.UserRepository;

@Service
public class UserService {

	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final int MAX_PASSWORD_LENGTH = 72;

	private static final Pattern PASSWORD_PATTERN = Pattern.compile(
			"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{}|;:,.<>/~])[A-Za-z\\d@$!%*?&#^()_+\\-=\\[\\]{}|;:,.<>/~]{8,72}$");

	private final UserRepository repo;
	private final PasswordEncoder encoder;
	private final JwtService jwtService;

	public UserService(UserRepository repo, PasswordEncoder encoder, JwtService jwtService) {

		this.repo = repo;
		this.encoder = encoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public User register(RegisterRequest request) {

		if (request.getEmail() == null || request.getEmail().isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}

		String email = request.getEmail().trim().toLowerCase();

		if (repo.existsByEmailIgnoreCase(email)) {
			throw new IllegalArgumentException("Email is already registered");
		}

		validatePassword(request.getPassword());

		User user = new User();

		user.setName(request.getName() != null ? request.getName().trim() : "");
		user.setEmail(email);
		user.setPassword(encoder.encode(request.getPassword()));
		user.setRole(request.getRole());

		user.setVerified(false);
		user.setNgoVerified(false);

		return repo.save(user);
	}

	public User get(Long id) {

		return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	public AuthResponse login(LoginRequest request) {

		if (request.getEmail() == null || request.getEmail().isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}

		String email = request.getEmail().trim().toLowerCase();

		User user = repo.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

		if (!encoder.matches(request.getPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Invalid email or password");
		}

		if (!user.isVerified()) {
			throw new IllegalArgumentException("Please verify your email before logging in");
		}

		String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

		return new AuthResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isVerified(),
				user.isNgoVerified(), token);
	}

	@Transactional
	public User submitNgoVerification(Long userId, String darpanId, String phone) {

		User user = repo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

		if (user.getRole() != Role.NGO) {
			throw new IllegalArgumentException("Only NGO accounts can submit NGO verification credentials");
		}

		user.setDarpanId(darpanId.trim().toUpperCase());
		user.setPhone(phone.trim());
		user.setNgoVerified(true);

		return repo.save(user);
	}

	public List<User> getVolunteers() {

		return repo.findByRole(Role.VOLUNTEER);
	}

	private void validatePassword(String password) {

		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException("Password is required");
		}

		if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
			throw new IllegalArgumentException("Password must be between 8 and 72 characters");
		}

		if (!PASSWORD_PATTERN.matcher(password).matches()) {
			throw new IllegalArgumentException(
					"Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
		}
	}
}