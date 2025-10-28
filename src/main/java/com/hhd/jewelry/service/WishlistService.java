package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Product;
import com.hhd.jewelry.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hhd.jewelry.entity.Wishlist;

import java.util.List;
import java.util.Optional;

public interface WishlistService {
    Page<Wishlist> getWishlistByUserId(int userId, Pageable pageable);
    boolean toggleWishlist(User user, Product product);
    List<Wishlist> getAllByUser(User user);
    Wishlist save(Wishlist wishlist);
    void delete(Wishlist wishlist);
    void deleteById(Long id);
    Optional<Wishlist> getWishlistByUserAndProduct(User user, Product product);
    Optional<Wishlist> getWishlistById(Integer id);
}
