package dev.guilherme.payments_flux.domain.repository;

import dev.guilherme.payments_flux.domain.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("SELECT w.balance FROM Wallet w WHERE w.id = :id")
    Optional<BigDecimal> findBalanceById(Long id);
}
