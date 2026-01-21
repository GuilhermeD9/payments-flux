package dev.guilherme.payments_flux.domain.service.wallet;

import dev.guilherme.payments_flux.api.dto.WalletDTO;
import dev.guilherme.payments_flux.api.mapper.WalletMapper;
import dev.guilherme.payments_flux.domain.entity.Wallet;
import dev.guilherme.payments_flux.domain.repository.WalletRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class WalletServiceImpl implements WalletService{

    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletMapper walletMapper;


    @Override
    public WalletDTO.Response create(WalletDTO.CreateRequest walletDTO) {
        Wallet newWallet = walletMapper.toEntity(walletDTO);
        newWallet.setPassword(passwordEncoder.encode(walletDTO.password()));
        newWallet.setBalance(BigDecimal.ZERO);

        Wallet savedWallet = walletRepository.save(newWallet);
        return walletMapper.toResponse(savedWallet);
    }
    
    @Override
    public WalletDTO.Response findById(Long id) {
        Wallet wallet = walletRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Wallet not found"));
        return walletMapper.toResponse(wallet);
    }
    
    @Override
    public WalletDTO.Response update(Long id, WalletDTO.UpdateRequest walletDTO) {
        Wallet wallet = walletRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Wallet not found"));
        
        walletMapper.updateEntity(walletDTO, wallet);
        
        Wallet updatedWallet = walletRepository.save(wallet);
        return walletMapper.toResponse(updatedWallet);
    }
    
    @Override
    public void delete(Long id) {
        if (!walletRepository.existsById(id)) {
            throw new RuntimeException("Wallet not found");
        }
        walletRepository.deleteById(id);
    }
}
