package com.shareplate.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shareplate.dto.AuthResponse;
import com.shareplate.entity.Role;
import com.shareplate.entity.User;
import com.shareplate.repository.UserRepository;
import com.shareplate.service.EmailVerificationService;
import com.shareplate.service.PasswordResetService;
import com.shareplate.service.SmsService;
import com.shareplate.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "https://shareplate-green.vercel.app"})
public class UserController {

	private final UserService service;
	private final EmailVerificationService emailVerificationService;
	private final PasswordResetService passwordResetService;
	private final UserRepository userRepository;
	private final SmsService smsService;

	public UserController(
			UserService service, 
			EmailVerificationService emailVerificationService,
			PasswordResetService passwordResetService,
			UserRepository userRepository,
			SmsService smsService) {

		this.service = service;
		this.emailVerificationService = emailVerificationService;
		this.passwordResetService = passwordResetService;
		this.userRepository = userRepository;
		this.smsService = smsService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {

		User savedUser = service.register(request);

		emailVerificationService.createAndSendVerificationCode(savedUser);

		return convertToResponse(savedUser);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {

		return service.login(request);
	}

	@PostMapping("/verify-email")
	public String verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {

		emailVerificationService.verifyEmail(request.getEmail(), request.getCode());

		return "Email verified successfully. You can now log in.";
	}

	@PostMapping("/resend-verification")
	public String resendVerification(@Valid @RequestBody ResendVerificationRequest request) {

		emailVerificationService.resendVerificationCode(request.getEmail());

		return "If the account exists and is not verified, a new verification code has been sent.";
	}

	@PostMapping("/forgot-password")
	public String forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {

		/*
		 * Always return the same response whether the email exists or not.
		 *
		 * This prevents people from discovering which email addresses have SharePlate
		 * accounts.
		 */
		passwordResetService.requestPasswordReset(request.getEmail());

		return "If an account exists for this email, a password reset link has been sent.";
	}

	@GetMapping("/validate-reset-token")
	public String validateResetToken(@RequestParam String token) {

		passwordResetService.validateResetToken(token);

		return "Password reset link is valid.";
	}

	@PostMapping("/reset-password")
	public String resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

		passwordResetService.resetPassword(request);

		return "Password reset successfully. You can now log in.";
	}

	/*
	 * Users may only retrieve their own profile.
	 *
	 * The ID supplied in the URL is compared against the authenticated user's JWT
	 * identity. This prevents one user from requesting another user's profile by
	 * changing the URL ID.
	 */
	@GetMapping("/{id}")
	public UserResponse get(@PathVariable Long id, Authentication authentication) {

		Long authenticatedUserId = getAuthenticatedUserId(authentication);

		if (!authenticatedUserId.equals(id)) {
			throw new IllegalArgumentException("You are not authorized to view this user");
		}

		User user = service.get(authenticatedUserId);

		return convertToResponse(user);
	}

	/*
	 * NGO Verification Submission Endpoint
	 */
	@PostMapping("/{id}/verify-ngo")
	public UserResponse verifyNgo(@PathVariable Long id, @Valid @RequestBody NgoVerificationRequest request,
			Authentication authentication) {

		Long authenticatedUserId = getAuthenticatedUserId(authentication);

		if (!authenticatedUserId.equals(id)) {
			throw new IllegalArgumentException("You are not authorized to submit verification for this user");
		}

		requireRole(authentication, Role.NGO);

		User updatedUser = service.submitNgoVerification(id, request.getDarpanId(), request.getPhone());

		return convertToResponse(updatedUser);
	}

	/*
	 * Only NGOs need the volunteer list because NGOs assign volunteers to claimed
	 * food listings.
	 */
	@GetMapping("/volunteers")
	public List<UserResponse> getVolunteers(Authentication authentication) {

		requireRole(authentication, Role.NGO);

		return service.getVolunteers().stream().map(this::convertToResponse).toList();
	}

	// 1. Send OTP (Enforcing phone uniqueness across users)
	@PostMapping("/{id}/send-phone-otp")
	public ResponseEntity<?> sendPhoneOtp(@PathVariable Long id, @RequestBody Map<String, String> body) {
		String rawPhone = body.get("phone");
		if (rawPhone == null || rawPhone.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
		}

		String cleanPhone = rawPhone.replaceAll("\\D", "").replaceFirst("^91", "");
		if (cleanPhone.length() != 10) {
			return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid 10-digit mobile number"));
		}

		// Check if phone number is already registered by another account
		userRepository.findByPhone(cleanPhone).ifPresent(existingUser -> {
			if (!existingUser.getId().equals(id)) {
				throw new IllegalArgumentException("This phone number is already registered to another account.");
			}
		});

		String formatted = smsService.generateAndSendOtp(cleanPhone);
		return ResponseEntity.ok(Map.of("message", "OTP sent successfully to " + formatted));
	}

	// 2. Verify OTP and commit phone to database
	@PostMapping("/{id}/verify-phone-otp")
	public ResponseEntity<?> verifyPhoneOtp(@PathVariable Long id, @RequestBody Map<String, String> body) {
		String rawPhone = body.get("phone");
		String code = body.get("code");

		if (rawPhone == null || code == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Phone and OTP code are required"));
		}

		String cleanPhone = rawPhone.replaceAll("\\D", "").replaceFirst("^91", "");

		if (!smsService.verifyOtp(cleanPhone, code)) {
			return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP code"));
		}

		// Enforce uniqueness check
		if (userRepository.findByPhone(cleanPhone).filter(u -> !u.getId().equals(id)).isPresent()) {
			return ResponseEntity.badRequest().body(Map.of("error", "This phone number is already registered to another account."));
		}

		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		user.setPhone(cleanPhone);
		userRepository.save(user);

		return ResponseEntity.ok(Map.of(
			"message", "Phone verified successfully!",
			"phone", cleanPhone
		));
	}

	private Long getAuthenticatedUserId(Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new IllegalArgumentException("Authentication is required");
		}

		Object principal = authentication.getPrincipal();

		if (principal instanceof Long) {
			return (Long) principal;
		}

		if (principal instanceof Integer) {
			return ((Integer) principal).longValue();
		}

		if (principal instanceof String) {
			try {
				return Long.parseLong((String) principal);
			} catch (NumberFormatException ignored) {
				// Continue below.
			}
		}

		throw new IllegalArgumentException("Unable to determine authenticated user");
	}

	private void requireRole(Authentication authentication, Role requiredRole) {

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new IllegalArgumentException("Authentication is required");
		}

		boolean hasRequiredRole = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.anyMatch(authority -> authority.equals("ROLE_" + requiredRole.name()));

		if (!hasRequiredRole) {
			throw new IllegalArgumentException("You are not authorized to perform this action");
		}
	}

	private UserResponse convertToResponse(User user) {

		return new UserResponse(
				user.getId(), 
				user.getName(), 
				user.getEmail(), 
				user.getRole(), 
				user.isVerified(),
				user.isNgoVerified(),
				user.getPhone()
		);
	}
}