package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.User;

public interface CartService {
    Cart getCartByUser(User user);
    void save(Cart cart);
    void delete(Cart cart);
}
