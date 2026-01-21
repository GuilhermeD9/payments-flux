package dev.guilherme.payments_flux.domain.service.transfer;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import jakarta.transaction.Transactional;

import java.util.UUID;

public interface TransferService {

    @Transactional
    TransferDTO.Response create(TransferDTO.CreateRequest transferDTO);
    
    TransferDTO.Response findById(UUID id);
}
