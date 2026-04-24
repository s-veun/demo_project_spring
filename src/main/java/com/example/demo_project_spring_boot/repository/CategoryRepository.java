package com.example.demo_project_spring_boot.repository;

import com.example.demo_project_spring_boot.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
     Boolean existsByCatName(String catName);
}
