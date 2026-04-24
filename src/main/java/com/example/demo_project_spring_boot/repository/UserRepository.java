package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.Enum.Role;
import com.example.demo_project_spring_boot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User>findByUsername(String username);
    List<User> findByRole(Role role);
}
