package com.shareplate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shareplate.entity.EmailVerificationToken;
import com.shareplate.entity.User;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

	Optional<EmailVerificationToken> findByUser(User user);

	Optional<EmailVerificationToken> findByUserId(Long userId);

	void deleteByUser(User user);

	void deleteByUserId(Long userId);
}