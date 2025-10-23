package com.hhd.jewelry.service;

import com.hhd.jewelry.dto.CheckoutDto;
import com.hhd.jewelry.entity.Address;
import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.entity.OrderItem;
import com.hhd.jewelry.entity.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    void save(Order order);
    boolean existsOrderByUser(User user);
    @Transactional
    void deleteById(Integer id);
    Optional<Order> getOrderByOrderId(Integer orderId);
    @Transactional
    Order createNewOrder(User user, CheckoutDto form);
    @Transactional
    Order updateOrderStatus(Integer orderId, Order.Status newStatus);
}
