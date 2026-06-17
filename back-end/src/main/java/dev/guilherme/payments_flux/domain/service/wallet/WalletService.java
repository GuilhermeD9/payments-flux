package dev.guilherme.payments_flux.domain.service.wallet;

import dev.guilherme.payments_flux.api.dto.WalletDTO;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    WalletDTO.Response create(WalletDTO.CreateRequest walletDTO);
    
    WalletDTO.Response findById(String id);

    List<WalletDTO.Response> findAll();

    BigDecimal getBalance(String id);

    WalletDTO.Response update(String id, WalletDTO.UpdateRequest walletDTO);
    
    void delete(String id);

    WalletDTO.Response deposit(String id, WalletDTO.MoneyRequest depositDTO);

    WalletDTO.Response withdraw(String id, WalletDTO.MoneyRequest withdrawDTO);
}
