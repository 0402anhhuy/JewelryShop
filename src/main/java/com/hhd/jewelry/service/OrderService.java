package com.hhd.jewelry.service;

import com.hhd.jewelry.dto.CheckoutDto;
import com.hhd.jewelry.entity.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {
    Order findById(Integer id);
    Optional<Order> getOrderByOrderId(Integer orderId);
    boolean existsOrderByUser(User user);

    @Transactional
    Order createNewOrder(User user, CheckoutDto form);

    @Transactional
    Order updateOrderStatus(Integer orderId, Order.Status newStatus);

    @Transactional
    void deleteById(Integer id);

    void saveAllReviews(ReviewRequest request, Map<Integer, List<MultipartFile>> filesMap);
    void save(Order order);
}