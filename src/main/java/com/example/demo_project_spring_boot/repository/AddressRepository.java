package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser_Id(Long userId);
    Optional<Address> findByAddressIdAndUser_Id(Long addressId, Long userId);
    Optional<Address> findByUser_IdAndIsDefaultTrue(Long userId);
}
