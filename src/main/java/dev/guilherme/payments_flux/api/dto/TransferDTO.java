package dev.guilherme.payments_flux.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferDTO(
        @NotNull Long senderId,
        @NotNull Long reciverId,
        @Positive BigDecimal value
) {
}
