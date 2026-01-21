package dev.guilherme.payments_flux.domain.service.transfer;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.domain.entity.Transfer;
import jakarta.transaction.Transactional;

public interface TransferService {

    @Transactional
    Transfer create(TransferDTO transferDTO);

}
