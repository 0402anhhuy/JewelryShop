package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Address;
import com.hhd.jewelry.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface AddressService {
    Page<Address> getAddressByUserId(int userId, Pageable pageable);
    List<Address> getAddressByUserId(int userId);
    List<Address> getAddressesByUser(User user);
    Address getAddressById(int id);
    Address save(Address address);
    void delete(Address address);
}
