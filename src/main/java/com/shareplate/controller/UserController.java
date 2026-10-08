package com.shareplate.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shareplate.entity.Role;
import com.shareplate.entity.User;
import com.shareplate.repository.UserRepository;
import com.shareplate.service.SmsService;
import com.shareplate.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "https://shareplate-green.vercel.app", "https://shareplate-kzf3j6zt6-share-plate.vercel.app", "https://shareplate-3r0hkrk8j-share-plate.vercel.app"})
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private SmsService smsService;

    // 1. REGISTER NEW USER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String roleStr = body.get("role");

        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, email, and password are required"));
        }

        Role role = Role.DONOR;
        if (roleStr != null) {
            try {
                role = Role.valueOf(roleStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        try {
            User registered = userService.register(name, email, password, role);
            return ResponseEntity.ok(registered);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Could not register user"));
        }
    }

    // 2. LOGIN USER
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        try {
            return ResponseEntity.ok(userService.login(email, password));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
    }

    // 3. VERIFY EMAIL
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and code are required"));
        }

        try {
            userService.verifyEmail(email, code);
            return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 4. RESEND VERIFICATION CODE
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        try {
            userService.resendVerificationCode(email);
            return ResponseEntity.ok(Map.of("message", "Verification code resent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 5. FORGOT PASSWORD
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        try {
            userService.requestPasswordReset(email);
            return ResponseEntity.ok(Map.of("message", "If an account exists, a reset link has been sent."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "If an account exists, a reset link has been sent."));
        }
    }

    // 6. VALIDATE RESET TOKEN
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        try {
            userService.validateResetToken(token);
            return ResponseEntity.ok(Map.of("valid", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 7. RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");

        try {
            userService.resetPassword(token, newPassword, confirmPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 8. GET SINGLE USER BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 9. GET ONLINE VOLUNTEERS (In-memory filter for volunteer dispatch)
    @GetMapping("/volunteers")
    public ResponseEntity<List<User>> getOnlineVolunteers() {
        List<User> volunteers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.VOLUNTEER && u.isOnline())
                .toList();
        return ResponseEntity.ok(volunteers);
    }

    // 10. SEND PHONE SMS OTP
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

        Optional<User> existing = userRepository.findByPhone(cleanPhone);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "This mobile number is already linked to another account"));
        }

        if (smsService != null) {
            String formatted = smsService.generateAndSendOtp(cleanPhone);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully to " + formatted));
        }

        return ResponseEntity.ok(Map.of("message", "OTP generated (dev mode fallback)"));
    }

    // 11. VERIFY PHONE OTP & COMMIT TO USER
    @PostMapping("/{id}/verify-phone-otp")
    public ResponseEntity<?> verifyPhoneOtp(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String rawPhone = body.get("phone");
        String code = body.get("code");

        if (rawPhone == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone and OTP code are required"));
        }

        String cleanPhone = rawPhone.replaceAll("\\D", "").replaceFirst("^91", "");

        if (smsService != null && !smsService.verifyOtp(cleanPhone, code)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP code"));
        }

        Optional<User> existing = userRepository.findByPhone(cleanPhone);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "This mobile number is already linked to another account"));
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPhone(cleanPhone);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Phone verified and linked successfully!",
            "phone", cleanPhone
        ));
    }

    // 12. NGO LEGAL VERIFICATION
    @PostMapping("/{id}/verify-ngo")
    public ResponseEntity<?> verifyNgo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String darpanId = body.get("darpanId");
        String phone = body.get("phone");

        if (darpanId == null || darpanId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "NGO Darpan ID or Registration Number is required"));
        }

        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Authorized mobile number is required"));
        }

        String cleanPhone = phone.replaceAll("\\D", "").replaceFirst("^91", "");
        if (cleanPhone.length() != 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid 10-digit mobile number"));
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<User> existing = userRepository.findByPhone(cleanPhone);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "This mobile number is already registered to another account"));
        }

        user.setPhone(cleanPhone);
        user.setNgoVerified(true);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "NGO verified successfully! Account is now authorized to claim listings.",
            "ngoVerified", true,
            "phone", cleanPhone
        ));
    }

    // 13. VOLUNTEER ONLINE/OFFLINE TOGGLE
    @PostMapping(value = {"/{id}/online-status", "/online-status"})
    public ResponseEntity<?> updateOnlineStatus(
            @PathVariable(required = false) Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long effectiveUserId = id;

        if (effectiveUserId == null && body.containsKey("userId")) {
            try {
                effectiveUserId = Long.valueOf(body.get("userId").toString());
            } catch (Exception ignored) {}
        }

        if (effectiveUserId == null && authentication != null && authentication.getPrincipal() != null) {
            try {
                effectiveUserId = Long.valueOf(authentication.getPrincipal().toString());
            } catch (Exception ignored) {}
        }

        if (effectiveUserId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User ID is required to toggle availability status"));
        }

        User user = userRepository.findById(effectiveUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (body.containsKey("online")) {
            user.setOnline(Boolean.parseBoolean(body.get("online").toString()));
        }

        if (body.containsKey("latitude") && body.get("latitude") != null) {
            try {
                user.setLatitude(Double.parseDouble(body.get("latitude").toString()));
            } catch (Exception ignored) {}
        }

        if (body.containsKey("longitude") && body.get("longitude") != null) {
            try {
                user.setLongitude(Double.parseDouble(body.get("longitude").toString()));
            } catch (Exception ignored) {}
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Duty status updated successfully",
            "online", user.isOnline()
        ));
    }
}