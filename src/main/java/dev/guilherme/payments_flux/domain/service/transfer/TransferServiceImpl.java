package dev.guilherme.payments_flux.domain.service.transfer;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.api.exception.BusinessException;
import dev.guilherme.payments_flux.api.exception.ResourceNotFoundException;
import dev.guilherme.payments_flux.api.mapper.TransferMapper;
import dev.guilherme.payments_flux.core.constraints.CacheNames;
import dev.guilherme.payments_flux.domain.entity.Transfer;
import dev.guilherme.payments_flux.domain.entity.Wallet;
import dev.guilherme.payments_flux.domain.repository.TransferRepository;
import dev.guilherme.payments_flux.domain.repository.WalletRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class  TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;
    private final TransferMapper transferMapper;

    @Override
    @CachePut(value = CacheNames.TRANSFER, key = "#result.id()")
    @CacheEvict(value = CacheNames.BALANCE, key = "#transferDTO.senderId() + ',' + #transferDTO.receiverId()")
    @Transactional
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
            walletRepository.saveAll(List.of(sender, receiver));
        } else {
            throw new BusinessException("Insufficient balance for transfer.");
        }

        Transfer newTransfer = transferMapper.toEntity(transferDTO);
        newTransfer.setSenderId(sender.getId());
        newTransfer.setReceiverId(receiver.getId());
        newTransfer.setCreatedAt(LocalDateTime.now());
        transferRepository.save(newTransfer);

        return transferMapper.toResponse(newTransfer);
    }
    
    @Override
    @Cacheable(value = CacheNames.TRANSFER, key = "#id")
    public TransferDTO.Response findById(String id) {
        Transfer transfer = transferRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transfer not found", id));
        return transferMapper.toResponse(transfer);
    }

    @Override
    public Page<TransferDTO.Response> findAll(Pageable pageable) {
        return transferRepository.findAll(pageable).map(transferMapper::toResponse);
    }

    @Override
    @Cacheable(value = CacheNames.TRANSFER, key = "#id")
    public List<TransferDTO.Response> findBySender(String id) {
        List<Transfer> transferBySenderId = transferRepository.findTransferBySenderId(id);
        return transferBySenderId.stream().map(transferMapper::toResponse).toList();
    }

    @Override
    @Cacheable(value = CacheNames.TRANSFER, key = "#id")
    public List<TransferDTO.Response> findByReceiver(String id) {
        List<Transfer> transferByReceiverId = transferRepository.findTransferByReceiverId(id);
        return transferByReceiverId.stream().map(transferMapper::toResponse).toList();
    }
}
