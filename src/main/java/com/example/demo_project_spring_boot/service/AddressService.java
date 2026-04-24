package com.example.demo_project_spring_boot.service;

import com.example.demo_project_spring_boot.dto.AddressDto;

import java.util.List;

public interface AddressService {
    AddressDto addAddress(AddressDto addressDTO);
    List<AddressDto> getAddressesByUserId(Long userId);
    AddressDto updateAddress(Long addressId, AddressDto addressDTO);
    void deleteAddress(Long addressId);

}
