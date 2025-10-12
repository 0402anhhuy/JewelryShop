package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Address;
import com.hhd.jewelry.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    Page<Address> findByUserId(int userId, Pageable pageable);
}
