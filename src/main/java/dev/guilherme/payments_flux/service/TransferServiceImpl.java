package dev.guilherme.payments_flux.service;

import dev.guilherme.payments_flux.dto.TransferDTO;
import dev.guilherme.payments_flux.entity.Transfer;
import dev.guilherme.payments_flux.entity.Wallet;
import dev.guilherme.payments_flux.exception.ServiceException;
import dev.guilherme.payments_flux.repository.TransferRepository;
import dev.guilherme.payments_flux.repository.WalletRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;

    @Override
    public Transfer create(TransferDTO transferDTO) {
        Wallet reciver = walletRepository.findById(transferDTO.reciverId()).orElseThrow(
                () -> new ServiceException("Wallet reciver with id %d not founded.".formatted(transferDTO.reciverId())));
        Wallet sender = walletRepository.findById(transferDTO.senderId()).orElseThrow(
                () -> new ServiceException("Wallet sender with id %d not founded.".formatted(transferDTO.senderId())));

        if (sender.getBalance().compareTo(transferDTO.value()) >= 0) {
            sender.setBalance(sender.getBalance().subtract(transferDTO.value()));
            reciver.setBalance(reciver.getBalance().add(transferDTO.value()));
        } else {
            throw new ServiceException("Value for transfer not valid.");
        }

        Transfer newTransfer = new Transfer();
        newTransfer.setSender(sender);
        newTransfer.setReceiver(reciver);
        newTransfer.setAmount(transferDTO.value());
        newTransfer.setCreatedAt(LocalDateTime.now());

        transferRepository.save(newTransfer);

        return newTransfer;
    }
}
