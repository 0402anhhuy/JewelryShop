package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Integer> {
    Optional<OtpToken> findTopByEmailAndUsedIsFalseOrderByIdDesc(String email);
}
