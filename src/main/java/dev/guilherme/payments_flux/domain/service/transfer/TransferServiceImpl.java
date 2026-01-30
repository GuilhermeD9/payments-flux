package dev.guilherme.payments_flux.domain.service.transfer;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.api.exception.BusinessException;
import dev.guilherme.payments_flux.api.exception.ResourceNotFoundException;
import dev.guilherme.payments_flux.api.mapper.TransferMapper;
import dev.guilherme.payments_flux.domain.entity.Transfer;
import dev.guilherme.payments_flux.domain.entity.Wallet;
import dev.guilherme.payments_flux.domain.repository.TransferRepository;
import dev.guilherme.payments_flux.domain.repository.WalletRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class  TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;
    private final TransferMapper transferMapper;

    @Override
    public TransferDTO.Response create(TransferDTO.CreateRequest transferDTO) {
        Wallet receiver = walletRepository.findById(transferDTO.receiverId()).orElseThrow(
                () -> new ResourceNotFoundException("Wallet receiver with id %d not found.", transferDTO.receiverId()));
        Wallet sender = walletRepository.findById(transferDTO.senderId()).orElseThrow(
                () -> new ResourceNotFoundException("Wallet sender with id %d not found.", transferDTO.senderId()));

        if (sender.getId().equals(receiver.getId())) {
            throw new BusinessException("The transferency is not be finished.");
        }

        if (sender.getBalance().compareTo(transferDTO.amount()) >= 0) {
            sender.setBalance(sender.getBalance().subtract(transferDTO.amount()));
            receiver.setBalance(receiver.getBalance().add(transferDTO.amount()));
        } else {
            throw new BusinessException("Insufficient balance for transfer.");
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
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found", id));
        return transferMapper.toResponse(transfer);
    }

    @Override
    public Page<TransferDTO.Response> findAll(Pageable pageable) {
        return transferRepository.findAll(pageable).map(transferMapper::toResponse);
    }

    @Override
    public List<TransferDTO.Response> findBySender(Long id) {
        List<Transfer> transferBySenderId = transferRepository.findTransferBySenderId(id);
        if (transferBySenderId.isEmpty()) {
            throw new ResourceNotFoundException("Transfer by sender not found", id);
        }
        return transferBySenderId.stream().map(transferMapper::toResponse).toList();
    }

    @Override
    public List<TransferDTO.Response> findByReceiver(Long id) {
        List<Transfer> transferByReceiverId = transferRepository.findTransferByReceiverId(id);
        if (transferByReceiverId.isEmpty()) {
            throw new ResourceNotFoundException("Transfer by receiver not found", id);
        }
        return transferByReceiverId.stream().map(transferMapper::toResponse).toList();
    }
}
