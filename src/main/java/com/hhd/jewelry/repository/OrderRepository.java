package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {
    @EntityGraph(attributePaths = {"user"})
    Page<Order> findAll(Pageable pageable);
    boolean existsByUser(User user);}
