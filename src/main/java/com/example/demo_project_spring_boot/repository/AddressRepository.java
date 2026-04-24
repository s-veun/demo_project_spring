package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);

    Long addressId(Long addressId);
}
