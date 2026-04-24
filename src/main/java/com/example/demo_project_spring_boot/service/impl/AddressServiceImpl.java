package com.example.demo_project_spring_boot.service.impl;

import com.example.demo_project_spring_boot.dto.AddressDto;
import com.example.demo_project_spring_boot.model.Address;
import com.example.demo_project_spring_boot.model.User;
import com.example.demo_project_spring_boot.repository.AddressRepository;
import com.example.demo_project_spring_boot.repository.UserRepository;
import com.example.demo_project_spring_boot.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public AddressDto addAddress(AddressDto addressDTO) {
        // 🌟 កែមកប្រើ getUserId() វិញ
        User existingUser = userRepository.findById(addressDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User ID " + addressDTO.getUserId() + " not found"));

        Address address = new Address();
        address.setFullName(addressDTO.getFullName());
        address.setPhoneNumber(addressDTO.getPhoneNumber());
        address.setCity(addressDTO.getCity());
        address.setDistrict(addressDTO.getDistrict());
        address.setDetailsAddress(addressDTO.getDetailsAddress());

        address.setUser(existingUser);

        Address savedAddress = addressRepository.save(address);

        return mapToDto(savedAddress);
    }

    @Override
    public List<AddressDto> getAddressesByUserId(Long userId) {
        // ពិនិត្យថាមាន User នោះមែនឬអត់
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("រកមិនឃើញអ្នកប្រើប្រាស់ ID: " + userId);
        }

        // ទាញយក List នៃ Entity រួចបំប្លែងទៅជា List នៃ DTO
        return addressRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public AddressDto updateAddress(Long addressId, AddressDto addressDTO) {
        // ១. ស្វែងរកអាសយដ្ឋានដែលមានស្រាប់
        Address existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("រកមិនឃើញអាសយដ្ឋាន ID: " + addressId));

        // ២. កែប្រែទិន្នន័យ (Update fields)
        copyDtoToEntity(addressDTO, existingAddress);

        // ៣. រក្សាទុកការកែប្រែ
        Address updatedAddress = addressRepository.save(existingAddress);

        return mapToDto(updatedAddress);
    }

    @Override
    public void deleteAddress(Long addressId) {
        if (!addressRepository.existsById(addressId)) {
            throw new RuntimeException("មិនអាចលុបបានទេ ព្រោះរកមិនឃើញអាសយដ្ឋាន ID: " + addressId);
        }
        addressRepository.deleteById(addressId);
    }

    // --- Helper Methods សម្រាប់ Mapping ---

    private void copyDtoToEntity(AddressDto dto, Address entity) {
        entity.setFullName(dto.getFullName());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setCity(dto.getCity());
        entity.setDistrict(dto.getDistrict());
        entity.setDetailsAddress(dto.getDetailsAddress());
    }

    private AddressDto mapToDto(Address entity) {
        AddressDto dto = new AddressDto();
        dto.setAddressId(entity.getAddressId());
        dto.setFullName(entity.getFullName());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setCity(entity.getCity());
        dto.setDistrict(entity.getDistrict());
        dto.setDetailsAddress(entity.getDetailsAddress());
        dto.setUserId(entity.getUser().getId());
        return dto;
    }
}