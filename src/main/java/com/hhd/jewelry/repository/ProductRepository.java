package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByName(String productName);
    List<Product> findAllBy();
    List<Product> findAllByCategory_Name(String categoryName);
    List<Product> findAllByCollection_Name(String collectionName);
    List<Product> findAllByCategory_NameAndCollection_Name(String categoryName, String collectionName);
    List<Product> findAllByPriceBetween(Integer priceAfter, Integer priceBefore);
    List<Product> findAllByStockQuantityGreaterThan(int stock);
    List<Product> findAllByMaterial(String material);
    Optional<Product> findBySerialNumber(String serialNumber);
    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE products AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();

}

