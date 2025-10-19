package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.CartItem;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.CartItemRepository;
import com.hhd.jewelry.service.CartItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartItemServiceImpl implements CartItemService {
    private final CartItemRepository cartItemRepository;
    @Override
    public int deleteSelectedByUser(User user) {
         return cartItemRepository.deleteSelectedByUser(user);
    }
}
