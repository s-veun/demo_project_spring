package com.example.demo_project_spring_boot.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    private String fullName;
    private String phoneNumber;
    private String country;
    private String city;
    private String state;
    private String district;
    private String postalCode;
    private String addressLine1;
    private String addressLine2;
    private String detailsAddress;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}
