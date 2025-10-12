package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Integer>{
    Page<Wishlist> findByUserId(int userId, Pageable pageable);
}
