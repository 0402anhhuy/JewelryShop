package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;

    public User findByEmailOrPhone(String input) {
        if (input == null || input.isBlank()) return null;
        if (input.contains("@")) {
            return userRepository.findByEmail(input).orElse(null);
        }
        else {
            return userRepository.findByPhone(input).orElse(null);
        }
    }
}