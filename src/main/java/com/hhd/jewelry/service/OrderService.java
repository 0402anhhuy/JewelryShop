package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.entity.User;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    void save(Order order);
    boolean existsOrderByUser(User user);
    Optional<Order> getOrderByOrderId(Integer orderId);
}
