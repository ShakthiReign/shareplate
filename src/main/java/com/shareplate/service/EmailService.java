package com.shareplate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

private static final Logger log = LoggerFactory.getLogger(EmailService.class);

private final JavaMailSender mailSender;
private final String fromEmail;
private final String frontendUrl;

public EmailService(JavaMailSender mailSender, 
@Value("${shareplate.mail.from:${SHAREPLATE_MAIL_USERNAME:no-reply@shareplate.com}}") String fromEmail,
@Value("${shareplate.frontend-url:http://localhost:5173}") String frontendUrl) {

this.mailSender = mailSender;
this.fromEmail = fromEmail;
this.frontendUrl = frontendUrl;
}

public void sendVerificationEmail(String recipientEmail, String recipientName, String verificationCode) {
System.out.println("==================================================================");
System.out.println(">>> VERIFICATION OTP FOR: " + recipientEmail);
System.out.println(">>> CODE: " + verificationCode);
System.out.println("==================================================================");

try {
SimpleMailMessage message = new SimpleMailMessage();
message.setFrom(fromEmail);
message.setTo(recipientEmail);
message.setSubject("SharePlate - Verify your email");

message.setText("Hi " + recipientName + ",\n\n" + "Welcome to SharePlate!\n\n"
+ "Your email verification code is:\n\n" + verificationCode + "\n\n"
+ "This code is valid for 10 minutes.\n\n" + "If you did not create a SharePlate account, "
+ "you can safely ignore this email.\n\n" + "Regards,\n" + "SharePlate Team");

mailSender.send(message);
log.info("Verification email sent successfully to {}", recipientEmail);
} catch (Exception e) {
log.error("SMTP delivery failed for {}: {}. OTP printed above for testing.", recipientEmail, e.getMessage());
}
}

public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) {
String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

System.out.println("==================================================================");
System.out.println(">>> PASSWORD RESET LINK FOR: " + recipientEmail);
System.out.println(">>> LINK: " + resetLink);
System.out.println("==================================================================");

try {
SimpleMailMessage message = new SimpleMailMessage();
message.setFrom(fromEmail);
message.setTo(recipientEmail);
message.setSubject("SharePlate - Reset your password");

message.setText("Hi " + recipientName + ",\n\n" + "We received a request to reset your SharePlate password.\n\n"
+ "Use the link below to create a new password:\n\n" + resetLink + "\n\n"
+ "This password reset link is valid for 30 minutes and can only be used once.\n\n"
+ "If you did not request a password reset, you can safely ignore this email.\n\n"
+ "Regards,\n" + "SharePlate Team");

mailSender.send(message);
log.info("Password reset email sent successfully to {}", recipientEmail);
} catch (Exception e) {
log.error("SMTP delivery failed for {}: {}. Link printed above for testing.", recipientEmail, e.getMessage());
}
}
}
