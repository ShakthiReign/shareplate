package com.shareplate.service;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shareplate.controller.LoginRequest;
import com.shareplate.controller.RegisterRequest;
import com.shareplate.dto.AuthResponse;
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

	@Autowired
	@Lazy
	private EmailVerificationService emailVerificationService;

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
		user.setOnline(false);

		// Keep null so PostgreSQL's unique constraint uk_users_phone doesn't conflict on empty strings
		if (request.getPhone() != null && !request.getPhone().isBlank()) {
			String cleanPhone = request.getPhone().replaceAll("\\D", "").replaceFirst("^91", "");
			if (cleanPhone.length() == 10) {
				user.setPhone(cleanPhone);
			}
		}

		User saved = repo.save(user);

		if (emailVerificationService != null) {
			try {
				emailVerificationService.createAndSendVerificationCode(saved);
			} catch (Exception e) {
				System.err.println("Failed to send verification email: " + e.getMessage());
			}
		}

		return saved;
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
			if (emailVerificationService != null) {
				try {
					emailVerificationService.resendVerificationCode(user.getEmail());
				} catch (Exception ignored) {}
			}
			throw new IllegalArgumentException("Please verify your email before signing in");
		}

		String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

		return new AuthResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				user.isVerified(),
				user.isNgoVerified(),
				token
		);
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
					"Password must contain uppercase, lowercase, number, and special character");
		}
	}
}