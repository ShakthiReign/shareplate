package com.shareplate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

private static final Logger log = LoggerFactory.getLogger(EmailService.class);
private final HttpClient httpClient = HttpClient.newHttpClient();

private final String apiKey;
private final String senderEmail;
private final String frontendUrl;

public EmailService(
@Value("${brevo.api-key:${BREVO_API_KEY:}}") String apiKey,
@Value("${brevo.sender.email:${BREVO_SENDER_EMAIL:shareplate.team@gmail.com}}") String senderEmail,
@Value("${shareplate.frontend-url:https://shareplate-green.vercel.app}") String frontendUrl) {
this.apiKey = apiKey;
this.senderEmail = senderEmail;
this.frontendUrl = frontendUrl;
}

public void sendVerificationEmail(String recipientEmail, String recipientName, String verificationCode) {
System.out.println("==================================================================");
System.out.println(">>> VERIFICATION OTP FOR: " + recipientEmail);
System.out.println(">>> SENDER: " + senderEmail);
System.out.println(">>> CODE: " + verificationCode);
System.out.println("==================================================================");

String jsonPayload = """
{
  "sender": {"name": "SharePlate", "email": "%s"},
  "to": [{"email": "%s", "name": "%s"}],
  "subject": "SharePlate - Verify your email",
  "htmlContent": "<div style='font-family: sans-serif; padding: 20px; color: #111;'><h2 style='color: #16a34a;'>Welcome to SharePlate!</h2><p>Hi %s,</p><p>Use this verification code to complete your signup:</p><h1 style='font-size: 32px; letter-spacing: 5px; color: #16a34a; background: #f0fdf4; display: inline-block; padding: 10px 20px; border-radius: 8px;'>%s</h1><p>This code expires in 10 minutes.</p></div>"
}
""".formatted(senderEmail, recipientEmail, recipientName, recipientName, verificationCode);

sendEmailRequest(jsonPayload, recipientEmail);
}

public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) {
String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

String jsonPayload = """
{
  "sender": {"name": "SharePlate", "email": "%s"},
  "to": [{"email": "%s", "name": "%s"}],
  "subject": "SharePlate - Reset your password",
  "htmlContent": "<div style='font-family: sans-serif; padding: 20px; color: #111;'><h2>Password Reset Request</h2><p>Hi %s,</p><p>Click below to reset your password:</p><p><a href='%s' style='background: #16a34a; color: white; padding: 10px 18px; text-decoration: none; border-radius: 6px; display: inline-block;'>Reset Password</a></p><p>This link expires in 30 minutes.</p></div>"
}
""".formatted(senderEmail, recipientEmail, recipientName, recipientName, resetLink);

sendEmailRequest(jsonPayload, recipientEmail);
}

private void sendEmailRequest(String jsonPayload, String recipientEmail) {
if (apiKey == null || apiKey.isBlank()) {
log.error("BREVO_API_KEY is missing from environment variables!");
return;
}

try {
HttpRequest request = HttpRequest.newBuilder()
.uri(URI.create("https://api.brevo.com/v3/smtp/email"))
.header("accept", "application/json")
.header("api-key", apiKey.trim())
.header("content-type", "application/json")
.POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
.build();

HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

if (response.statusCode() >= 200 && response.statusCode() < 300) {
log.info("Brevo email dispatched successfully to {}. Response: {}", recipientEmail, response.body());
} else {
log.error("Brevo API error (Status {}): {}", response.statusCode(), response.body());
}
} catch (Exception e) {
log.error("Failed to connect to Brevo API: {}", e.getMessage(), e);
}
}
}