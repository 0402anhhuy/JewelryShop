package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.Wishlist;
import com.hhd.jewelry.repository.WishlistRepository;
import com.hhd.jewelry.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;

    @Override
    public Page<Wishlist> getWishlistByUserId(int userId, Pageable pageable) {
        return wishlistRepository.findByUserId(userId, pageable);
    }
}
