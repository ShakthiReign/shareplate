package com.shareplate.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.shareplate.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authorizationHeader = request.getHeader("Authorization");

		/*
		 * No Authorization header.
		 *
		 * Continue normally.
		 *
		 * Public endpoints such as:
		 *
		 * POST /api/users/login POST /api/users/register
		 *
		 * do not need a JWT.
		 */
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {

			filterChain.doFilter(request, response);
			return;
		}

		/*
		 * Remove "Bearer " from the Authorization header.
		 *
		 * Example:
		 *
		 * Bearer eyJhbGciOi...
		 *
		 * becomes:
		 *
		 * eyJhbGciOi...
		 */
		String token = authorizationHeader.substring(7).trim();

		/*
		 * Empty token is not valid.
		 */
		if (token.isEmpty()) {

			filterChain.doFilter(request, response);
			return;
		}

		try {

			/*
			 * Verify:
			 *
			 * - JWT signature - JWT expiration - other validation implemented by JwtService
			 */
			if (jwtService.isTokenValid(token)) {

				/*
				 * Don't replace an authentication that another security mechanism may already
				 * have established.
				 */
				if (SecurityContextHolder.getContext().getAuthentication() == null) {

					Long userId = jwtService.extractUserId(token);

					String role = jwtService.extractRole(token);

					/*
					 * A valid JWT should contain both a user ID and a role.
					 */
					if (userId != null && role != null && !role.isBlank()) {

						/*
						 * Spring Security expects roles with the ROLE_ prefix.
						 *
						 * DONOR becomes ROLE_DONOR
						 */
						SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

						/*
						 * Principal = authenticated user's ID.
						 *
						 * This is important:
						 *
						 * Controllers/services can now obtain the authenticated user ID from the JWT
						 * instead of trusting an ID supplied by the frontend.
						 */
						UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
								userId, null, List.of(authority));

						/*
						 * Store authentication for the current request.
						 */
						SecurityContextHolder.getContext().setAuthentication(authentication);
					}
				}
			}

		} catch (Exception e) {

			/*
			 * Invalid / expired / malformed JWT.
			 *
			 * Do not allow a partially parsed token to authenticate the request.
			 *
			 * Protected endpoints will subsequently be rejected by Spring Security.
			 */
			SecurityContextHolder.clearContext();
		}

		/*
		 * Continue to the controller.
		 */
		filterChain.doFilter(request, response);
	}
}