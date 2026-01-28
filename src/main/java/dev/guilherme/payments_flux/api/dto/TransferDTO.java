package dev.guilherme.payments_flux.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferDTO(
    UUID id,
    @NotNull Long senderId,
    @NotNull Long receiverId,
    @NotNull @Positive BigDecimal amount,
    LocalDateTime createdAt
) {
    
    public record CreateRequest(
        @NotNull Long senderId,
        @NotNull Long receiverId,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal amount
    ) {}
    
    public record Response(
        UUID id,
        Long senderId,
        Long receiverId,
        BigDecimal amount,
        LocalDateTime createdAt
    ) {}
}
