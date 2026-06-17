package dev.guilherme.payments_flux.domain.repository;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.domain.entity.Transfer;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransferRepository extends MongoRepository<Transfer, String> {

    @Query(value = "{ 'senderId' : ?0 }")
    List<Transfer> findTransferBySenderId(String id);

    @Query(value = "{ 'receiverId' : ?0 }")
    List<Transfer> findTransferByReceiverId(String id);

    @Aggregation(pipeline = { """
            { $match: {
                createdAt: { $gte: ?0, $lte: ?1 },
                type: { $in: ['TRANSFER', 'DEPOSIT', 'WITHDRAW'] }
            } },
            { $group: {
                _id: '$type',
                totalAmount: { $sum: '$amount' },
                count: { $sum: 1 }
            } },
            { $project: {
                _id: 0,
                operationType: '$_id',
                totalAmount: '$totalAmount',
                count: '$count'
            } }
            """
    }) List<TransferDTO.FinancialSummary> getFinancialSummary(LocalDate startDate, LocalDate endDate);
}