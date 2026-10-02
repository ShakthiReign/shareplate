package com.shareplate.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey signingKey;

	private final long expirationTime;

	public JwtService(@Value("${shareplate.jwt.secret}") String secret,
			@Value("${shareplate.jwt.expiration-ms}") long expirationTime) {

		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT secret is missing. Configure shareplate.jwt.secret.");
		}

		/*
		 * HMAC-SHA signing requires a sufficiently strong key.
		 *
		 * The exact minimum depends on the JJWT algorithm/key being used. Keeping a
		 * strong production secret is essential.
		 */
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT secret must be at least 32 bytes long.");
		}

		if (expirationTime <= 0) {
			throw new IllegalStateException("JWT expiration must be greater than zero.");
		}

		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

		this.expirationTime = expirationTime;
	}

	// --------------------------------------------------
	// CREATE JWT TOKEN
	// --------------------------------------------------

	public String generateToken(Long userId, String email, String role) {

		Date issuedAt = new Date();

		Date expiration = new Date(issuedAt.getTime() + expirationTime);

		return Jwts.builder()
				/*
				 * JWT subject = authenticated user's database ID.
				 */
				.subject(String.valueOf(userId))

				/*
				 * Kept for compatibility with the current application.
				 */
				.claim("email", email)

				/*
				 * Used by JwtAuthenticationFilter to create:
				 *
				 * ROLE_DONOR ROLE_NGO ROLE_VOLUNTEER
				 */
				.claim("role", role)

				.issuedAt(issuedAt).expiration(expiration)

				/*
				 * Sign the JWT.
				 */
				.signWith(signingKey)

				.compact();
	}

	// --------------------------------------------------
	// READ CLAIMS
	// --------------------------------------------------

	public Claims extractClaims(String token) {

		return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
	}

	// --------------------------------------------------
	// EXTRACT USER ID
	// --------------------------------------------------

	public Long extractUserId(String token) {

		String subject = extractClaims(token).getSubject();

		if (subject == null || subject.isBlank()) {
			throw new IllegalArgumentException("JWT does not contain a user ID.");
		}

		return Long.valueOf(subject);
	}

	// --------------------------------------------------
	// EXTRACT EMAIL
	// --------------------------------------------------

	public String extractEmail(String token) {

		return extractClaims(token).get("email", String.class);
	}

	// --------------------------------------------------
	// EXTRACT ROLE
	// --------------------------------------------------

	public String extractRole(String token) {

		return extractClaims(token).get("role", String.class);
	}

	// --------------------------------------------------
	// CHECK TOKEN VALIDITY
	// --------------------------------------------------

	public boolean isTokenValid(String token) {

		try {

			/*
			 * parseSignedClaims() verifies the signature and validates registered claims
			 * such as expiration.
			 */
			extractClaims(token);

			return true;

		} catch (Exception e) {

			return false;
		}
	}
}