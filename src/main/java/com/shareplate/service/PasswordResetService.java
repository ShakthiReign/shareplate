package com.shareplate.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shareplate.controller.ResetPasswordRequest;
import com.shareplate.entity.PasswordResetToken;
import com.shareplate.entity.User;
import com.shareplate.repository.PasswordResetTokenRepository;
import com.shareplate.repository.UserRepository;

@Service
public class PasswordResetService {

	private static final int TOKEN_BYTES = 32;
	private static final int TOKEN_EXPIRY_MINUTES = 30;
	private static final int MAX_ATTEMPTS = 5;
	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final int MAX_PASSWORD_LENGTH = 72;

	// Requires: min 8 chars, max 72 chars, at least 1 uppercase, 1 lowercase, 1
	// digit, and 1 special character
	private static final Pattern PASSWORD_PATTERN = Pattern.compile(
			"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=\\[\\]{}|;:,.<>/~])[A-Za-z\\d@$!%*?&#^()_+\\-=\\[\\]{}|;:,.<>/~]{8,72}$");

	private final PasswordResetTokenRepository tokenRepository;
	private final UserRepository userRepository;
	private final EmailService emailService;
	private final PasswordEncoder passwordEncoder;

	private final SecureRandom secureRandom = new SecureRandom();

	public PasswordResetService(PasswordResetTokenRepository tokenRepository, UserRepository userRepository,
			EmailService emailService, PasswordEncoder passwordEncoder) {

		this.tokenRepository = tokenRepository;
		this.userRepository = userRepository;
		this.emailService = emailService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void requestPasswordReset(String email) {

		String normalizedEmail = normalizeEmail(email);

		User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

		/*
		 * Never reveal whether the email exists to prevent enumeration.
		 */
		if (user == null) {
			return;
		}

		/*
		 * Only one reset token should exist for the user. Requesting a new reset
		 * invalidates the previous one.
		 */
		tokenRepository.deleteByUserId(user.getId());

		String rawToken = generateSecureToken();
		String tokenHash = hashToken(rawToken);

		PasswordResetToken resetToken = new PasswordResetToken();
		resetToken.setUser(user);
		resetToken.setTokenHash(tokenHash);
		resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
		resetToken.setUsed(false);
		resetToken.setAttempts(0);
		resetToken.setCreatedAt(LocalDateTime.now());

		tokenRepository.save(resetToken);

		/*
		 * The raw token is sent only through the email. The database stores only the
		 * SHA-256 hash.
		 */
		emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);
	}

	@Transactional(readOnly = true)
	public void validateResetToken(String rawToken) {

		findValidToken(rawToken);
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {

		String rawToken = request.getToken();
		PasswordResetToken resetToken = findValidToken(rawToken);

		String newPassword = request.getNewPassword();
		String confirmPassword = request.getConfirmPassword();

		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException("Passwords do not match");
		}

		validatePassword(newPassword);

		/*
		 * Record this reset attempt.
		 */
		resetToken.setAttempts(resetToken.getAttempts() + 1);

		if (resetToken.getAttempts() >= MAX_ATTEMPTS) {
			resetToken.setUsed(true);
			tokenRepository.save(resetToken);
			throw new IllegalArgumentException("Too many reset attempts. Please request a new password reset link");
		}

		User user = resetToken.getUser();

		/*
		 * Replace old password with a BCrypt hash.
		 */
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);

		/*
		 * Invalidate token permanently.
		 */
		resetToken.setUsed(true);
		tokenRepository.save(resetToken);
	}

	private PasswordResetToken findValidToken(String rawToken) {

		if (rawToken == null || rawToken.isBlank()) {
			throw new IllegalArgumentException("Invalid password reset link");
		}

		String tokenHash = hashToken(rawToken);

		/*
		 * Direct database lookup by token hash.
		 */
		PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset link"));

		if (resetToken.isUsed()) {
			throw new IllegalArgumentException("This password reset link has already been used");
		}

		if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			tokenRepository.delete(resetToken);
			throw new IllegalArgumentException("This password reset link has expired");
		}

		if (resetToken.getAttempts() >= MAX_ATTEMPTS) {
			throw new IllegalArgumentException("Too many reset attempts. Please request a new password reset link");
		}

		return resetToken;
	}

	private String generateSecureToken() {

		byte[] tokenBytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(tokenBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}

	private String hashToken(String rawToken) {

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Unable to secure password reset token", exception);
		}
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

	private String normalizeEmail(String email) {

		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}

		return email.trim().toLowerCase();
	}
}