package dev.guilherme.payments_flux.domain.repository;

import dev.guilherme.payments_flux.domain.entity.Transfer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends MongoRepository<Transfer, String> {

    @Query(value = "{ 'senderId' : ?0 }")
    List<Transfer> findTransferBySenderId(String id);

    @Query(value = "{ 'receiverId' : ?0 }")
    List<Transfer> findTransferByReceiverId(String id);
}