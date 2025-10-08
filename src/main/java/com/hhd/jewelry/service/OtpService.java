package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.OtpToken;
import com.hhd.jewelry.repository.OtpTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {
    private final OtpTokenRepository repo;
    private final MailService email;
    private final SecureRandom rnd = new SecureRandom();

    @Value("${app.otp.ttl-minutes:5}")
    private int ttlMinutes;

    public OtpService(OtpTokenRepository repo, MailService email) {
        this.repo = repo; this.email = email;
    }

    public void sendOtp(String emailAddr){
        String code = String.format("%06d", rnd.nextInt(1_000_000));
        OtpToken t = new OtpToken();
        t.setEmail(emailAddr);
        t.setCode(code);
        t.setExpiresAt(LocalDateTime.now().plusMinutes(ttlMinutes));
        repo.save(t);

        email.send(emailAddr,
                "Mã xác thực đăng ký (OTP)",
                "Mã OTP của bạn là: " + code + " (hết hạn trong " + ttlMinutes + " phút).");
    }

    public boolean verify(String emailAddr, String code){
        var tk = repo.findTopByEmailAndUsedIsFalseOrderByIdDesc(emailAddr).orElse(null);
        if (tk == null) return false;
        if (tk.getExpiresAt().isBefore(LocalDateTime.now())) return false;
        if (!tk.getCode().equals(code)) return false;
        tk.setUsed(true);
        repo.save(tk);
        return true;
    }
}
