package com.example.demo_project_spring_boot.controller;

import com.example.demo_project_spring_boot.dto.AddressDto;
import com.example.demo_project_spring_boot.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // ១. បង្កើតអាសយដ្ឋានថ្មី
    @PostMapping("/add")
    public ResponseEntity<AddressDto> createAddress(@RequestBody AddressDto addressDto) {
        AddressDto savedAddress = addressService.addAddress(addressDto);
        return new ResponseEntity<>(savedAddress, HttpStatus.CREATED);
    }

    // ២. ទាញយកអាសយដ្ឋានទាំងអស់របស់ User ម្នាក់ (តាម userId)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AddressDto>> getAddressesByUserId(@PathVariable Long userId) {
        List<AddressDto> addresses = addressService.getAddressesByUserId(userId);
        return ResponseEntity.ok(addresses);
    }

    // ៣. កែប្រែអាសយដ្ឋាន (តាម addressId)
    @PutMapping("/update/{addressId}")
    public ResponseEntity<AddressDto> updateAddress(
            @PathVariable Long addressId,
            @RequestBody AddressDto addressDto) {
        AddressDto updated = addressService.updateAddress(addressId, addressDto);
        return ResponseEntity.ok(updated);
    }

    // ៤. លុបអាសយដ្ឋាន (តាម addressId)
    @DeleteMapping("/delete/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId) {
        addressService.deleteAddress(addressId);
        return ResponseEntity.ok("Address deleted successfully with ID: " + addressId);
    }
}
