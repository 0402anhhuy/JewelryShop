package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.entity.Wishlist;
import com.hhd.jewelry.repository.WishlistRepository;
import com.hhd.jewelry.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;

    @Override
    public Page<Wishlist> getWishlistByUserId(int userId, Pageable pageable) {
        return wishlistRepository.findByUserId(userId, pageable);
    }

    @Override
    public boolean toggleWishlist(User user, Product product) {
        var existing = wishlistRepository.findByUserAndProduct(user, product);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return false; // đã xoá
        } else {
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            wishlist.setProduct(product);
            wishlistRepository.save(wishlist);
            return true; // đã thêm
        }
    }

    @Override
    public List<Wishlist> getAllByUser(User user) {
        return wishlistRepository.findAllByUser(user);
    }
}
