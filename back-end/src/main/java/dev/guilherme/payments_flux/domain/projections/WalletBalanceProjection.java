package dev.guilherme.payments_flux.domain.projections;

import java.math.BigDecimal;

public record WalletBalanceProjection(BigDecimal balance) {}
