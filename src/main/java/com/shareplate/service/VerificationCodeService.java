package com.shareplate.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class VerificationCodeService {

	private final SecureRandom random = new SecureRandom();

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private final SecretKeySpec encryptionKey;

	public VerificationCodeService(@Value("${shareplate.jwt.secret}") String secret) {

		try {
			/*
			 * We derive a fixed 256-bit AES key from the existing SharePlate application
			 * secret.
			 *
			 * The actual JWT secret is never used directly as the AES key.
			 */
			byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));

			this.encryptionKey = new SecretKeySpec(keyBytes, "AES");

		} catch (Exception e) {
			throw new IllegalStateException("Unable to initialize verification-code encryption", e);
		}
	}

	/**
	 * Generates a cryptographically secure 6-digit verification code.
	 *
	 * Example: 483921
	 */
	public String generateCode() {

		return String.format("%06d", random.nextInt(1_000_000));
	}

	/**
	 * Converts the verification code into a one-way BCrypt hash.
	 *
	 * The original code cannot be recovered from the hash.
	 */
	public String hashCode(String code) {

		if (code == null || code.trim().isEmpty()) {
			throw new IllegalArgumentException("Verification code cannot be empty");
		}

		return passwordEncoder.encode(code.trim());
	}

	/**
	 * Checks whether the supplied code matches the stored BCrypt hash.
	 */
	public boolean matches(String code, String hash) {

		if (code == null || code.trim().isEmpty()) {
			return false;
		}

		if (hash == null || hash.trim().isEmpty()) {
			return false;
		}

		return passwordEncoder.matches(code.trim(), hash);
	}

	/**
	 * Encrypts the actual verification code so that it can later be recovered by
	 * the authorized backend.
	 *
	 * AES-GCM provides confidentiality and integrity.
	 */
	public String encryptCode(String code) {

		if (code == null || code.trim().isEmpty()) {
			throw new IllegalArgumentException("Verification code cannot be empty");
		}

		try {
			byte[] iv = new byte[12];
			random.nextBytes(iv);

			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

			GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

			cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, parameterSpec);

			byte[] encrypted = cipher.doFinal(code.trim().getBytes(StandardCharsets.UTF_8));

			/*
			 * Store:
			 *
			 * IV + encrypted data
			 *
			 * separated by a dot.
			 */
			return Base64.getEncoder().encodeToString(iv) + "." + Base64.getEncoder().encodeToString(encrypted);

		} catch (Exception e) {
			throw new IllegalStateException("Unable to encrypt verification code", e);
		}
	}

	/**
	 * Decrypts an encrypted verification code.
	 */
	public String decryptCode(String encryptedCode) {

		if (encryptedCode == null || encryptedCode.trim().isEmpty()) {
			throw new IllegalArgumentException("Encrypted verification code cannot be empty");
		}

		try {
			String[] parts = encryptedCode.split("\\.", 2);

			if (parts.length != 2) {
				throw new IllegalArgumentException("Invalid encrypted verification code");
			}

			byte[] iv = Base64.getDecoder().decode(parts[0]);

			byte[] encrypted = Base64.getDecoder().decode(parts[1]);

			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

			GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

			cipher.init(Cipher.DECRYPT_MODE, encryptionKey, parameterSpec);

			byte[] decrypted = cipher.doFinal(encrypted);

			return new String(decrypted, StandardCharsets.UTF_8);

		} catch (Exception e) {
			throw new IllegalStateException("Unable to decrypt verification code", e);
		}
	}
}