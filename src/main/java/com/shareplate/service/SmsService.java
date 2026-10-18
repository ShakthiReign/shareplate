package com.shareplate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private static final int OTP_EXPIRY_MINUTES = 5;

    @Value("${fast2sms.api.key:}")
    private String apiKey;

    // Stores: phone -> {code, expiresAt}
    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    private static class OtpEntry {
        String code;
        long expiresAt;

        OtpEntry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    public String generateAndSendOtp(String rawPhone) {
        String cleanPhone = rawPhone.replaceAll("\\D", "");
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }

        String otp = String.format("%06d", new SecureRandom().nextInt(1000000));
        otpStorage.put(cleanPhone, new OtpEntry(otp, System.currentTimeMillis() + (OTP_EXPIRY_MINUTES * 60 * 1000)));

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String smsText = "Your Shareplate phone number verification OTP is : " + otp + 
                                 "\nThis OTP is valid for " + OTP_EXPIRY_MINUTES + " minutes.";

                String encodedMessage = URLEncoder.encode(smsText, StandardCharsets.UTF_8);

                // Using Fast2SMS Quick Route ('q') which bypasses website/DLT verification
                String requestUrl = "https://www.fast2sms.com/dev/bulkV2?authorization=" + apiKey.trim()
                        + "&route=q"
                        + "&message=" + encodedMessage
                        + "&language=english"
                        + "&flash=0"
                        + "&numbers=" + cleanPhone;

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(requestUrl))
                        .header("authorization", apiKey.trim())
                        .header("cache-control", "no-cache")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Fast2SMS OTP dispatched to {}. Response: {}", cleanPhone, response.body());
            } catch (Exception e) {
                log.error("Fast2SMS dispatch failed to {}: {}", cleanPhone, e.getMessage(), e);
                log.info("[BACKUP OTP LOG] Phone: {} | OTP: {}", cleanPhone, otp);
            }
        } else {
            log.info("==================================================");
            log.info("SHAREPLATE FAST2SMS DEV OTP for {}: {}", cleanPhone, otp);
            log.info("==================================================");
        }

        return cleanPhone;
    }

    public boolean verifyOtp(String rawPhone, String code) {
        String cleanPhone = rawPhone.replaceAll("\\D", "");
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }

        OtpEntry entry = otpStorage.get(cleanPhone);
        if (entry == null) return false;

        if (System.currentTimeMillis() > entry.expiresAt) {
            otpStorage.remove(cleanPhone);
            return false;
        }

        if (entry.code.equals(code != null ? code.trim() : "")) {
            otpStorage.remove(cleanPhone);
            return true;
        }

        return false;
    }
}