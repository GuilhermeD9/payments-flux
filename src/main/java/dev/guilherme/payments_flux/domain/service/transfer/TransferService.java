package dev.guilherme.payments_flux.domain.service.transfer;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TransferService {

    @Transactional
    TransferDTO.Response create(TransferDTO.CreateRequest transferDTO);
    
    TransferDTO.Response findById(UUID id);

    Page<TransferDTO.Response> findAll(Pageable pageable);

    List<TransferDTO.Response> findBySender(Long id);

    List<TransferDTO.Response> findByReceiver(Long id);
}
