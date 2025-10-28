package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.User;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface UserService {
    Page<User> getUsersPage(int page, int size);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmailAndIdNot(String email, Integer id);
    boolean existsByPhoneAndIdNot(String phone, Integer id);
    void save(User user);
    void delete(User user);
    void updatePassword(String email, String newPassword);
}
