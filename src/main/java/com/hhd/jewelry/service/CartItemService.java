package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.CartItem;
import com.hhd.jewelry.entity.User;

import java.util.List;

public interface CartItemService {
    int deleteSelectedByUser(User user);
    boolean existsById(Integer itemId);
    void deleteById(Integer itemId);
    List<CartItem>  findAllByCart_CartId(Integer id);
}
