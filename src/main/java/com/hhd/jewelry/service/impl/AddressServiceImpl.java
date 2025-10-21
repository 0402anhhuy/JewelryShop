package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.entity.Address;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.repository.AddressRepository;
import com.hhd.jewelry.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    @Override
    public Page<Address> getAddressByUserId(int userId, Pageable pageable) {
        return addressRepository.findByUserId(userId, pageable);
    }

    @Override
    public List<Address> getAddressByUserId(int userId) {
        return addressRepository.findByUserId(userId);
    }

    @Override
    public List<Address> getAddressesByUser(User user) {
        return addressRepository.findAddressesByUser(user);
    }


    public Optional<Address> getAddressByIdAndUser(Integer id, User  user) {
        return addressRepository.findAddressByIdAndUser(id, user);
    }

    @Override
    public Address getAddressById(int id) {
        return addressRepository.findAddressById(id);
    }


    @Override
    public Address save(Address address) {
        return addressRepository.save(address);
    }

    @Override
    public void delete(Address address) {
        addressRepository.delete(address);
    }
}
