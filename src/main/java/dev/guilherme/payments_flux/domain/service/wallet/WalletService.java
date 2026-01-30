package dev.guilherme.payments_flux.domain.service.wallet;

import dev.guilherme.payments_flux.api.dto.WalletDTO;
import jakarta.transaction.Transactional;

import java.util.List;

public interface WalletService {

    @Transactional
    WalletDTO.Response create(WalletDTO.CreateRequest walletDTO);
    
    WalletDTO.Response findById(Long id);

    List<WalletDTO.Response> findAll();

    @Transactional
    WalletDTO.Response update(Long id, WalletDTO.UpdateRequest walletDTO);
    
    void delete(Long id);

    @Transactional
    WalletDTO.Response deposit(Long id, WalletDTO.MoneyRequest depositDTO);

    @Transactional
    WalletDTO.Response withdraw(Long id, WalletDTO.MoneyRequest withdrawDTO);
}
