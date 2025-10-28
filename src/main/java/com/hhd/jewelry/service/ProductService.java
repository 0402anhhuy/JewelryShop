package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProductByName(String productName);
    Product getProductBySerialNumber(String serialNumber);
    Product getProductById(Long id);
    List<Product> getProductsById(List<Long> id);
    List<Product> getProductsByCategoryName(String categoryName);
    List<Product> getProductsByCollectionName(String collectionName);
    List<Product> getProductsByCategoryNameAndCollectionName(String categoryName, String collectionName);
    List<Product> getProductsByPriceBetween(Integer minPrice, Integer maxPrice);
    List<Product> getProductsByStockQuantityGreaterThan(int stock);
    List<Product> getProductsByMaterial(String material);
    void save(Product product);
    void delete(Product product);
    void deleteAll();
    void resetAutoIncrement();
    void AddProductToCart(String email, String serialNumber);
    void RemoveProductToCart(Integer cart_id, Integer product_id);
    @Transactional(rollbackFor = Exception.class)
    void updateStockForOrder(Order order);
    Page<Product> findProducts(Specification<Product> spec, Pageable pageable);
    List<Product> findAllProducts(Specification<Product> spec, Sort sort);
}
