package com.hhd.jewelry.repository;

import com.hhd.jewelry.entity.Address;
import com.hhd.jewelry.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    Page<Address> findByUserId(int userId, Pageable pageable);
    List<Address> findByUserId(int userId);
    List<Address> findAddressesByUser(User user);
    Optional<Address> findAddressByIdAndUser(Integer id, User user);
    Address findById(int id);

    Address findAddressById(Integer id);
}
