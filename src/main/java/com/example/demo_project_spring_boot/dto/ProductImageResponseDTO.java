package com.example.demo_project_spring_boot.dto;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductImageResponseDTO {
    private Long id;

    private String imageUrl;

    private String publicId;

    private Boolean thumbnail;

    private Integer sortOrder;

    private String altText;
}
