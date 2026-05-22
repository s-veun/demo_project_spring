package com.example.demo_project_spring_boot.dto.admin.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GeneralSettingsDto {

    @NotBlank(message = "Website name is required")
    private String websiteName;

    @NotBlank(message = "Website description is required")
    private String websiteDescription;

    @Email(message = "Contact email must be valid")
    @NotBlank(message = "Contact email is required")
    private String contactEmail;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[+0-9()\\-\\s]{7,20}$", message = "Contact phone format is invalid")
    private String contactPhone;

    @NotBlank(message = "Address is required")
    private String address;

    private String companyInformation;

    @NotBlank(message = "Timezone is required")
    private String timezone;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Date format is required")
    private String dateFormat;
}

