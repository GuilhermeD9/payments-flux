package dev.guilherme.payments_flux.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferDTO(
    String id,
    @NotBlank String senderId,
    @NotBlank String receiverId,
    @NotNull @Positive BigDecimal amount,
    LocalDateTime createdAt
) {
    
    public record CreateRequest(
        @NotBlank String senderId,
        @NotBlank String receiverId,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal amount
    ) {}
    
    public record Response(
        String id,
        String senderId,
        String receiverId,
        BigDecimal amount,
        LocalDateTime createdAt
    ) {}
}
