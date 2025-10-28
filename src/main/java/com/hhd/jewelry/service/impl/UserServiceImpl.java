package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.UserRepository;
import com.hhd.jewelry.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<User> getUsersPage(int page, int size) {
        return null;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Integer id) {
        return userRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public boolean existsByPhoneAndIdNot(String phone, Integer id) {
        return userRepository.existsByPhoneAndIdNot(phone, id);
    }

    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    public void delete(User user) {
        userRepository.delete(user);
    }

    @Override
    public void updatePassword(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
        } else {
            throw new RuntimeException("Không tìm thấy người dùng với email: " + email);
        }
    }
}
