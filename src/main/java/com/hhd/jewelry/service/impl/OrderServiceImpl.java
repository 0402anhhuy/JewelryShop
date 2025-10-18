package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.OrderRepository;
import com.hhd.jewelry.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    @Override
    public void save(Order order) {
        orderRepository.save(order);
    }

    @Override
    public boolean existsOrderByUser(User user) {
        List<Order>  orders = orderRepository.findAllByUser(user);
        return orders.stream().anyMatch(Order::isPaid);
    }
}
