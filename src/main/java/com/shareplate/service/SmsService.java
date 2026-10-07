package com.shareplate.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.phone.number:}")
    private String fromNumber;

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
        if (cleanPhone.length() == 10) {
            cleanPhone = "+91" + cleanPhone;
        } else if (!cleanPhone.startsWith("+")) {
            cleanPhone = "+" + cleanPhone;
        }

        // 6-digit numeric OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(1000000));
        otpStorage.put(cleanPhone, new OtpEntry(otp, System.currentTimeMillis() + (5 * 60 * 1000)));

        if (accountSid != null && !accountSid.isBlank() && !accountSid.contains("your_account_sid")) {
            try {
                Twilio.init(accountSid, authToken);
                Message.creator(
                    new PhoneNumber(cleanPhone),
                    new PhoneNumber(fromNumber),
                    "Your SharePlate verification OTP is: " + otp + ". Valid for 5 minutes."
                ).create();
                log.info("SMS OTP successfully sent via Twilio to {}", cleanPhone);
            } catch (Exception e) {
                log.error("Twilio send failed: {}", e.getMessage());
            }
        } else {
            // Direct developer fallback printed in Render console logs
            log.info("==================================================");
            log.info("SHAREPLATE DEV SMS OTP for {}: {}", cleanPhone, otp);
            log.info("==================================================");
        }

        return cleanPhone;
    }

    public boolean verifyOtp(String rawPhone, String code) {
        String cleanPhone = rawPhone.replaceAll("\\D", "");
        if (cleanPhone.length() == 10) cleanPhone = "+91" + cleanPhone;
        else if (!cleanPhone.startsWith("+")) cleanPhone = "+" + cleanPhone;

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