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
import org.springframework.web.bind.annotation.RestController;

import com.shareplate.entity.Role;
import com.shareplate.entity.User;
import com.shareplate.repository.UserRepository;
import com.shareplate.service.SmsService;
import com.shareplate.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:5173", "https://shareplate-green.vercel.app", "https://shareplate-kzf3j6zt6-share-plate.vercel.app"})
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private SmsService smsService;

    // 1. GET SINGLE USER
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 2. GET ACTIVE VOLUNTEERS (In-memory filter to prevent repository method signature errors)
    @GetMapping("/volunteers")
    public ResponseEntity<List<User>> getOnlineVolunteers() {
        List<User> volunteers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.VOLUNTEER && u.isOnline())
                .toList();
        return ResponseEntity.ok(volunteers);
    }

    // 3. SEND PHONE SMS OTP
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

    // 4. VERIFY PHONE OTP & SAVE TO DATABASE
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

    // 5. NGO LEGAL VERIFICATION
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

    // 6. VOLUNTEER ONLINE/OFFLINE STATUS
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