package dev.guilherme.payments_flux.api.dto;

import dev.guilherme.payments_flux.core.validator.CPFCNPJ;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record WalletDTO(
    Long id,
    @NotBlank String fullName,
    @NotBlank @CPFCNPJ String cpfCnpj,
    @NotBlank @Email @Size(max = 120) String email,
    String password,
    @NotNull @DecimalMin("0.00") BigDecimal balance
) {
    
    public record CreateRequest(
        @NotBlank String fullName,
        @NotBlank @CPFCNPJ String cpfCnpj,
        @NotBlank @Email @Size(max = 40) String email,
        @NotBlank @Size(min = 6) String password
    ) {}
    
    public record UpdateRequest(
        @NotBlank String fullName,
        @NotBlank @Email @Size(max = 40) String email
    ) {}
    
    public record Response(
        Long id,
        String fullName,
        String cpfCnpj,
        String email,
        BigDecimal balance
    ) {}
}
