package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.CartRepository;
import com.hhd.jewelry.service.CartItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartItemServiceImpl implements CartItemService {
    private final CartRepository  cartRepository;
    @Override
    public void deleteByUser(User user) {
        if (user == null) return;
        Cart cart = cartRepository.findByUser(user);
        if (cart != null && cart.getItems() != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
    }
}
