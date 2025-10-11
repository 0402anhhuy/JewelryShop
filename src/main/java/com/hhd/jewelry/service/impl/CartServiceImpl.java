package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.CartRepository;
import com.hhd.jewelry.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    @Override
    public Cart getCartByUser(User user) {
        return cartRepository.findByUser(user);
    }

    @Override
    public void save(Cart cart) {
        cartRepository.save(cart);
    }

    @Override
    public void delete(Cart cart) {
        cartRepository.delete(cart);
    }
}
