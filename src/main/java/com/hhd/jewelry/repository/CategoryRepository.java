package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByName(String categoryName);
    List<Category> findAllBy();
}
