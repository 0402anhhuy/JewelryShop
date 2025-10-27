package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart,Integer> {
    Cart findByUser(User user);
    boolean existsByUser(User user);
}
