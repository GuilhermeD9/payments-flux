package dev.guilherme.payments_flux.domain.service.transfer;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.api.exception.ServiceException;
import dev.guilherme.payments_flux.api.mapper.TransferMapper;
import dev.guilherme.payments_flux.domain.entity.Transfer;
import dev.guilherme.payments_flux.domain.entity.Wallet;
import dev.guilherme.payments_flux.domain.repository.TransferRepository;
import dev.guilherme.payments_flux.domain.repository.WalletRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;
    private final TransferMapper transferMapper;

    @Override
    public TransferDTO.Response create(TransferDTO.CreateRequest transferDTO) {
        Wallet receiver = walletRepository.findById(transferDTO.receiverId()).orElseThrow(
                () -> new ServiceException("Wallet receiver with id %d not found.".formatted(transferDTO.receiverId())));
        Wallet sender = walletRepository.findById(transferDTO.senderId()).orElseThrow(
                () -> new ServiceException("Wallet sender with id %d not found.".formatted(transferDTO.senderId())));

        if (sender.getBalance().compareTo(transferDTO.amount()) >= 0) {
            sender.setBalance(sender.getBalance().subtract(transferDTO.amount()));
            receiver.setBalance(receiver.getBalance().add(transferDTO.amount()));
        } else if (sender.getId().equals(receiver.getId())) {
            throw new ServiceException("The transferency is not be finished.");
        } else {
            throw new ServiceException("Insufficient balance for transfer.");
        }

        Transfer newTransfer = transferMapper.toEntity(transferDTO);
        newTransfer.setSender(sender);
        newTransfer.setReceiver(receiver);
        newTransfer.setCreatedAt(LocalDateTime.now());
        transferRepository.save(newTransfer);

        return transferMapper.toResponse(newTransfer);
    }
    
    @Override
    public TransferDTO.Response findById(UUID id) {
        Transfer transfer = transferRepository.findById(id)
            .orElseThrow(() -> new ServiceException("Transfer not found"));
        return transferMapper.toResponse(transfer);
    }
}
