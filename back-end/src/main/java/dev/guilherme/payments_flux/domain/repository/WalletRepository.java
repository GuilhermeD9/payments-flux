package dev.guilherme.payments_flux.domain.repository;

import dev.guilherme.payments_flux.domain.entity.Wallet;
import dev.guilherme.payments_flux.domain.projections.WalletBalanceProjection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends MongoRepository<Wallet, String> {

    @Query(value = "{ '_id' : ?0 }", fields = "{ 'balance' : 1, '_id' : 0 }")
    Optional<WalletBalanceProjection> findBalanceById(String id);
}
