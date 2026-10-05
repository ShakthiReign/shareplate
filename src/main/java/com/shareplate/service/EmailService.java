package com.shareplate.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

private static final Logger log = LoggerFactory.getLogger(EmailService.class);

private final Resend resend;
private final String frontendUrl;

public EmailService(
@Value("${resend.api-key:${RESEND_API_KEY:}}") String apiKey,
@Value("${shareplate.frontend-url:https://shareplate-green.vercel.app}") String frontendUrl) {
this.resend = new Resend(apiKey);
this.frontendUrl = frontendUrl;
}

public void sendVerificationEmail(String recipientEmail, String recipientName, String verificationCode) {
log.info("Sending OTP verification to {}", recipientEmail);

try {
CreateEmailOptions params = CreateEmailOptions.builder()
.from("SharePlate <onboarding@resend.dev>")
.to(recipientEmail)
.subject("SharePlate - Verify your email")
.html("<p>Hi <strong>" + recipientName + "</strong>,</p>"
+ "<p>Welcome to SharePlate! Your email verification code is:</p>"
+ "<h2 style='letter-spacing: 4px; color: #16a34a;'>" + verificationCode + "</h2>"
+ "<p>This code is valid for 10 minutes.</p>")
.build();

CreateEmailResponse response = resend.emails().send(params);
log.info("OTP email delivered successfully! Resend ID: {}", response.getId());
} catch (Exception e) {
log.error("Failed to deliver email via Resend API: {}", e.getMessage(), e);
}
}

public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) {
String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

try {
CreateEmailOptions params = CreateEmailOptions.builder()
.from("SharePlate <onboarding@resend.dev>")
.to(recipientEmail)
.subject("SharePlate - Reset your password")
.html("<p>Hi <strong>" + recipientName + "</strong>,</p>"
+ "<p>Click the link below to reset your password:</p>"
+ "<p><a href='" + resetLink + "'>Reset Password</a></p>"
+ "<p>This link expires in 30 minutes.</p>")
.build();

resend.emails().send(params);
log.info("Password reset email sent to {}", recipientEmail);
} catch (Exception e) {
log.error("Failed to send password reset: {}", e.getMessage(), e);
}
}
}