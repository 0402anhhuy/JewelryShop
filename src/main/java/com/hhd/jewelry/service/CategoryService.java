package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Category;

import java.util.List;

public interface CategoryService {
    Category getCategoryByName(String categoryName);
    List<Category> getAllCategories();
    void save(Category category);
    void delete(Category category);
}
