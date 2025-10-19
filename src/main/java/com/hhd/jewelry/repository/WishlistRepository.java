package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Integer>{
    Page<Wishlist> findByUserId(int userId, Pageable pageable);
    Optional<Wishlist> findByUserAndProduct(User user, Product product);

    List<Wishlist> findAllByUser(User user);

}
