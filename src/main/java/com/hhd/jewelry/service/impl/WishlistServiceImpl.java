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
import java.util.Optional;


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

    @Override
    public Wishlist save(Wishlist wishlist) {
        return wishlistRepository.save(wishlist);
    }

    @Override
    public void delete(Wishlist wishlist) {
        wishlistRepository.delete(wishlist);
    }

    @Override
    public void deleteById(Long id) {

    }

    @Override
    public Optional<Wishlist> getWishlistByUserAndProduct(User user, Product product) {
        return wishlistRepository.findByUserAndProduct(user, product);
    }

    @Override
    public Optional<Wishlist> getWishlistById(Integer id) {
        return wishlistRepository.findById(id);
    }
}
