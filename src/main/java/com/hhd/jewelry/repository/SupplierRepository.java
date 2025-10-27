package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Query("SELECT COUNT(p) FROM Product p WHERE p.supplier.id = :supplierId")
    int countProductsBySupplier(@Param("supplierId") Long supplierId);
}
