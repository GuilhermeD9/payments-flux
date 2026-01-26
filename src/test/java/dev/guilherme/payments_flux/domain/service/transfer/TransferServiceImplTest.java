package dev.guilherme.payments_flux.domain.service.transfer;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.api.exception.BusinessException;
import dev.guilherme.payments_flux.api.exception.ResourceNotFoundException;
import dev.guilherme.payments_flux.api.mapper.TransferMapper;
import dev.guilherme.payments_flux.domain.entity.Transfer;
import dev.guilherme.payments_flux.domain.entity.Wallet;
import dev.guilherme.payments_flux.domain.repository.TransferRepository;
import dev.guilherme.payments_flux.domain.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransferMapper transferMapper;

    @InjectMocks
    private TransferServiceImpl transferService;

    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private Wallet sender;
    private Wallet receiver;

    @BeforeEach
    void setUp() {
        senderId = 1L;
        receiverId = 2L;
        amount = new BigDecimal("100.00");
        sender = new Wallet();
        receiver = new Wallet();
    }


    @Nested
    class SuccessCases {
        @Test
        void createTransferWithValidDataShouldReturnTransferResponse() {
            TransferDTO.CreateRequest createRequestDTO = new TransferDTO.CreateRequest(senderId, receiverId, amount);
            sender.setId(senderId);
            sender.setBalance(BigDecimal.valueOf(500));
            receiver.setId(receiverId);
            receiver.setBalance(BigDecimal.valueOf(300));

            Transfer savedTransfer = new Transfer();
            savedTransfer.setId(UUID.randomUUID());
            savedTransfer.setSender(sender);
            savedTransfer.setReceiver(receiver);
            savedTransfer.setAmount(amount);
            savedTransfer.setCreatedAt(LocalDateTime.now());

            TransferDTO.Response expectedResponse = new TransferDTO.Response(
                    savedTransfer.getId(), senderId, receiverId, amount, savedTransfer.getCreatedAt()
            );

            when(walletRepository.findById(createRequestDTO.senderId())).thenReturn(Optional.of(sender));
            when(walletRepository.findById(createRequestDTO.receiverId())).thenReturn(Optional.of(receiver));
            when(transferMapper.toEntity(createRequestDTO)).thenReturn(savedTransfer);
            when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);
            when(transferMapper.toResponse(savedTransfer)).thenReturn(expectedResponse);

            TransferDTO.Response response = transferService.create(createRequestDTO);

            assertNotNull(response);
            assertEquals(expectedResponse.id(), response.id());
            assertNotEquals(BigDecimal.valueOf(300), receiver.getBalance());
            assertNotEquals(BigDecimal.valueOf(400), sender.getBalance());
        }

        @Test
        void findTransferByIdShouldReturnTransferResponse() {
            UUID transferId = UUID.randomUUID();

            Transfer transfer = new Transfer();
            transfer.setId(transferId);
            transfer.setSender(sender);
            transfer.setReceiver(receiver);
            transfer.setAmount(amount);
            transfer.setCreatedAt(LocalDateTime.now());

            TransferDTO.Response expectedResponse = new TransferDTO.Response(
                    transferId, senderId, receiverId, amount, transfer.getCreatedAt()
            );

            when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
            when(transferMapper.toResponse(transfer)).thenReturn(expectedResponse);

            TransferDTO.Response response = transferService.findById(transferId);

            assertNotNull(response);
            assertEquals(expectedResponse.id(), response.id());
            verify(transferRepository).findById(transferId);
            verify(transferMapper).toResponse(transfer);
        }
    }

    @Nested
    class FailureCases {
        @Test
        void createTransferWithInsufficientBalanceShouldThrowBusinessException() {
            BigDecimal insufficientAmount = new BigDecimal("1000.00");
            TransferDTO.CreateRequest createRequestDTO = new TransferDTO.CreateRequest(senderId, receiverId, insufficientAmount);

            sender.setId(senderId);
            sender.setBalance(BigDecimal.valueOf(500.00));
            receiver.setId(receiverId);
            receiver.setBalance(BigDecimal.valueOf(200.00));

            when(walletRepository.findById(createRequestDTO.senderId())).thenReturn(Optional.of(sender));
            when(walletRepository.findById(createRequestDTO.receiverId())).thenReturn(Optional.of(receiver));

            BusinessException exception = assertThrows(
                    BusinessException.class, (() -> transferService.create(createRequestDTO))
            );

            assertEquals("Insufficient balance for transfer.", exception.getMessage());
            verify(transferRepository, never()).save(any());
        }

        @Test
        void createTransferWithSameIdShouldThrowBusinessException() {
            TransferDTO.CreateRequest createRequestDTO = new TransferDTO.CreateRequest(receiverId, receiverId, amount);

            Wallet wallet = new Wallet();
            wallet.setId(receiverId);
            wallet.setBalance(BigDecimal.valueOf(500));

            when(walletRepository.findById(createRequestDTO.senderId())).thenReturn(Optional.of(wallet));
            when(walletRepository.findById(createRequestDTO.receiverId())).thenReturn(Optional.of(wallet));

            BusinessException exception = assertThrows(
                    BusinessException.class, (() -> transferService.create(createRequestDTO))
            );

            assertEquals("The transferency is not be finished.", exception.getMessage());
            verify(transferRepository, never()).save(any());
        }

        @Test
        void findTransferByIdWhetNotFoundShouldThrowResourceNotFoundEx() {
            UUID transferId = UUID.randomUUID();

            when(transferRepository.findById(transferId)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class, () -> transferService.findById(transferId)
            );

            assertTrue(exception.getMessage().contains("Transfer not found"));
            verify(transferMapper, never()).toResponse(any());
        }
    }
}
