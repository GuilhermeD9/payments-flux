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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    class CreateTransfer {
        @Test
        @DisplayName("Should create transfer with valid data and return response")
        void shouldCreateTransferWithValidDataAndReturnResponse() {
            var requestDTO = new TransferDTO.CreateRequest(senderId, receiverId, amount);

            sender = new Wallet();
            sender.setId(senderId);
            sender.setBalance(BigDecimal.valueOf(500));

            receiver = new Wallet();
            receiver.setId(receiverId);
            receiver.setBalance(BigDecimal.valueOf(300));

            Transfer transferEntity = new Transfer();
            transferEntity.setAmount(amount);

            when(walletRepository.findById(senderId)).thenReturn(Optional.of(sender));
            when(walletRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
            when(transferMapper.toEntity(requestDTO)).thenReturn(transferEntity);
            when(transferRepository.save(any())).thenReturn(new Transfer(
                    UUID.randomUUID(), sender, receiver, amount, LocalDateTime.now()));
            when(transferMapper.toResponse(any())).thenReturn(new TransferDTO.Response(
                    UUID.randomUUID(), senderId, receiverId, amount, LocalDateTime.now()));

            var response = transferService.create(requestDTO);

            assertNotNull(response);
            ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
            verify(transferRepository).save(transferCaptor.capture());
            assertEquals(amount, transferCaptor.getValue().getAmount());

            verify(walletRepository, never()).save(any());
            assertEquals(0, sender.getBalance().compareTo(BigDecimal.valueOf(400)),
                    "Sender balance should be updated");
            assertEquals(0, receiver.getBalance().compareTo(BigDecimal.valueOf(400)),
                    "Receiver balance should be updated");
        }

        @Test
        @DisplayName("Should throw BusinessException when insufficient balance")
        void shouldThrowBusinessExcpetionWhenInsufficientBalance() {
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
        @DisplayName("Should throw BusinessException when transferring to same wallet")
        void shouldThrowBusinessExceptionWhenTransferringToSameWallet() {
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
    }

    @Nested
    class FindTransferById {
        @Test
        @DisplayName("Should find transfer by ID and return response")
        void shouldFindTransferByIdAndReturnResponse() {
            var id = UUID.randomUUID();
            var expectedResponse = new TransferDTO.Response(id, senderId, receiverId, amount, LocalDateTime.now());

            when(transferRepository.findById(id)).thenReturn(Optional.of(new Transfer()));
            when(transferMapper.toResponse(any())).thenReturn(expectedResponse);

            var response = transferService.findById(id);

            assertEquals(expectedResponse, response);
            verify(transferRepository).findById(id);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when transfer not found")
        void shouldThrowResourceNotFoundExceptionWhenTransferNotFound() {
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
