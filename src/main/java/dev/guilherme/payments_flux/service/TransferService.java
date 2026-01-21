package dev.guilherme.payments_flux.service;

import dev.guilherme.payments_flux.dto.TransferDTO;
import dev.guilherme.payments_flux.entity.Transfer;
import jakarta.transaction.Transactional;

public interface TransferService {

    @Transactional
    Transfer create(TransferDTO transferDTO);

}
