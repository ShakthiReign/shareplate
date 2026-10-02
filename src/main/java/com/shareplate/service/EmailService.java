package com.shareplate.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	private final JavaMailSender mailSender;
	private final String fromEmail;
	private final String frontendUrl;

	public EmailService(JavaMailSender mailSender, @Value("${shareplate.mail.from}") String fromEmail,
			@Value("${shareplate.frontend-url:http://localhost:5173}") String frontendUrl) {

		this.mailSender = mailSender;
		this.fromEmail = fromEmail;
		this.frontendUrl = frontendUrl;
	}

	public void sendVerificationEmail(String recipientEmail, String recipientName, String verificationCode) {

		SimpleMailMessage message = new SimpleMailMessage();

		message.setFrom(fromEmail);
		message.setTo(recipientEmail);
		message.setSubject("SharePlate - Verify your email");

		message.setText("Hi " + recipientName + ",\n\n" + "Welcome to SharePlate!\n\n"
				+ "Your email verification code is:\n\n" + verificationCode + "\n\n"
				+ "This code is valid for 10 minutes.\n\n" + "If you did not create a SharePlate account, "
				+ "you can safely ignore this email.\n\n" + "Regards,\n" + "SharePlate Team");

		mailSender.send(message);
	}

	public void sendPasswordResetEmail(String recipientEmail, String recipientName, String resetToken) {

		String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

		SimpleMailMessage message = new SimpleMailMessage();

		message.setFrom(fromEmail);
		message.setTo(recipientEmail);
		message.setSubject("SharePlate - Reset your password");

		message.setText("Hi " + recipientName + ",\n\n" + "We received a request to reset your SharePlate password.\n\n"
				+ "Use the link below to create a new password:\n\n" + resetLink + "\n\n"
				+ "This password reset link is valid for 30 minutes " + "and can only be used once.\n\n"
				+ "If you did not request a password reset, " + "you can safely ignore this email. "
				+ "Your current password will remain unchanged.\n\n" + "Regards,\n" + "SharePlate Team");

		mailSender.send(message);
	}
}