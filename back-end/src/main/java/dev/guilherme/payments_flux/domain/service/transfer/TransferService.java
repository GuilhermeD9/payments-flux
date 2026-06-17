package dev.guilherme.payments_flux.domain.service.transfer;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransferService {

    TransferDTO.Response create(TransferDTO.CreateRequest transferDTO);
    
    TransferDTO.Response findById(String id);

    Page<TransferDTO.Response> findAll(Pageable pageable);

    List<TransferDTO.Response> findBySender(String id);

    List<TransferDTO.Response> findByReceiver(String id);

    List<TransferDTO.FinancialSummary> getFinancialSummary(TransferDTO.FinancialSummaryRequest request);
}
