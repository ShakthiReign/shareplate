package com.shareplate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shareplate.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByUserId(Long userId);

	Optional<PasswordResetToken> findByTokenHash(String tokenHash);

	void deleteByUserId(Long userId);
}