package com.hhd.jewelry.service;

import com.hhd.jewelry.entity.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface AddressService {
    Page<Address> getAddressByUserId(int userId, Pageable pageable);
    Address getAddressById(int id);
    void save(Address address);
    void delete(Address address);
}
