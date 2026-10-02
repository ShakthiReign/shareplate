package com.shareplate.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shareplate.entity.EmailVerificationToken;
import com.shareplate.entity.User;
import com.shareplate.repository.EmailVerificationTokenRepository;
import com.shareplate.repository.UserRepository;

@Service
public class EmailVerificationService {

	private static final int CODE_LENGTH = 6;
	private static final int CODE_EXPIRY_MINUTES = 10;
	private static final int MAX_ATTEMPTS = 5;

	private final EmailVerificationTokenRepository tokenRepository;
	private final UserRepository userRepository;
	private final EmailService emailService;
	private final BCryptPasswordEncoder encoder;

	private final SecureRandom secureRandom = new SecureRandom();

	public EmailVerificationService(EmailVerificationTokenRepository tokenRepository, UserRepository userRepository,
			EmailService emailService, BCryptPasswordEncoder encoder) {

		this.tokenRepository = tokenRepository;
		this.userRepository = userRepository;
		this.emailService = emailService;
		this.encoder = encoder;
	}

	@Transactional
	public void createAndSendVerificationCode(User user) {

		if (user.isVerified()) {
			throw new IllegalArgumentException("Email is already verified");
		}

		// Remove any previous verification token.
		tokenRepository.deleteByUser(user);

		String code = generateCode();

		EmailVerificationToken token = new EmailVerificationToken();

		token.setUser(user);
		token.setTokenHash(encoder.encode(code));
		token.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES));
		token.setUsed(false);
		token.setAttempts(0);

		tokenRepository.save(token);

		emailService.sendVerificationEmail(user.getEmail(), user.getName(), code);
	}

	@Transactional
	public void verifyEmail(String email, String code) {

		String normalizedEmail = normalizeEmail(email);

		User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
				.orElseThrow(() -> new IllegalArgumentException("Invalid verification request"));

		if (user.isVerified()) {
			throw new IllegalArgumentException("Email is already verified");
		}

		EmailVerificationToken token = tokenRepository.findByUser(user)
				.orElseThrow(() -> new IllegalArgumentException("Verification code not found or expired"));

		if (token.isUsed()) {
			throw new IllegalArgumentException("Verification code has already been used");
		}

		if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
			tokenRepository.delete(token);

			throw new IllegalArgumentException("Verification code has expired");
		}

		if (token.getAttempts() >= MAX_ATTEMPTS) {
			throw new IllegalArgumentException("Too many incorrect attempts. Please request a new code");
		}

		token.setAttempts(token.getAttempts() + 1);

		if (!encoder.matches(code, token.getTokenHash())) {
			tokenRepository.save(token);

			throw new IllegalArgumentException("Invalid verification code");
		}

		user.setVerified(true);
		userRepository.save(user);

		token.setUsed(true);
		tokenRepository.save(token);
	}

	@Transactional
	public void resendVerificationCode(String email) {

		String normalizedEmail = normalizeEmail(email);

		User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
				.orElseThrow(() -> new IllegalArgumentException(
						"If an account exists for this email, " + "a verification code will be sent"));

		if (user.isVerified()) {
			throw new IllegalArgumentException("Email is already verified");
		}

		createAndSendVerificationCode(user);
	}

	private String generateCode() {
		int minimum = (int) Math.pow(10, CODE_LENGTH - 1);
		int maximum = (int) Math.pow(10, CODE_LENGTH) - 1;

		return String.valueOf(secureRandom.nextInt(maximum - minimum + 1) + minimum);
	}

	private String normalizeEmail(String email) {

		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}

		return email.trim().toLowerCase();
	}
}