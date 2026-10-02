package com.shareplate.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {

		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable())

				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authorizeHttpRequests(auth -> auth

						// =========================================
						// PUBLIC AUTHENTICATION ENDPOINTS
						// =========================================

						.requestMatchers(HttpMethod.POST, "/api/users/register", "/api/users/login",
								"/api/users/verify-email", "/api/users/resend-verification",
								"/api/users/forgot-password", "/api/users/reset-password")
						.permitAll()

						.requestMatchers(HttpMethod.GET, "/api/users/validate-reset-token").permitAll()

						// =========================================
						// ROLE-SPECIFIC ENDPOINTS
						// =========================================

						.requestMatchers(HttpMethod.GET, "/api/users/volunteers").hasRole("NGO")

						.requestMatchers(HttpMethod.GET, "/api/tasks/*/pickup-code").hasRole("DONOR")

						.requestMatchers(HttpMethod.GET, "/api/tasks/*/delivery-code").hasRole("NGO")

						.requestMatchers(HttpMethod.POST, "/api/tasks/*/collect").hasRole("VOLUNTEER")

						.requestMatchers(HttpMethod.POST, "/api/tasks/*/deliver").hasRole("VOLUNTEER")

						// =========================================
						// EVERYTHING ELSE REQUIRES LOGIN
						// =========================================

						.anyRequest().authenticated())

				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of("http://localhost:5173"));

		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}