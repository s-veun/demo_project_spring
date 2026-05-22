package com.example.demo_project_spring_boot.dto.admin.settings;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentSettingsDto {

    @NotNull
    private Boolean bakongEnabled;

    private String bakongMerchantId;

    @NotNull
    private Boolean abaEnabled;

    private String abaMerchantId;

    @NotNull
    private Boolean stripeEnabled;

    private String stripePublicKey;

    private String stripeSecretKey;

    @NotNull
    private Boolean paypalEnabled;

    private String paypalClientId;

    private String paypalSecret;

    @NotBlank
    private String baseCurrency;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal transactionFeePercent;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal flatTransactionFee;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal exchangeRateUsd;
}

