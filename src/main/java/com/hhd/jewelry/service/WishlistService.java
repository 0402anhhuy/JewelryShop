package com.hhd.jewelry.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hhd.jewelry.entity.Wishlist;

public interface WishlistService {
    Page<Wishlist> getWishlistByUserId(int userId, Pageable pageable);
}
