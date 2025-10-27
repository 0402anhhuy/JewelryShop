package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.Category;
import com.hhd.jewelry.repository.CategoryRepository;
import com.hhd.jewelry.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category getCategoryByName(String categoryName) {
        return categoryRepository.findByName(categoryName).orElse(null);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAllBy();
    }

    @Override
    public void save(Category category) {
        Optional<Category> existingCategory = categoryRepository.findByName(category.getName());

        if (existingCategory.isPresent()) {
            Category existing = existingCategory.get();
            existing.setImageUrl(category.getImageUrl());
            categoryRepository.save(existing);
        }
        else{
            categoryRepository.save(category);
        }
    }


    @Override
    public void delete(Category category) {
        categoryRepository.delete(category);
    }
}
