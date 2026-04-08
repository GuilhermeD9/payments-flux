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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    private String transferId;
    private String senderId;
    private String receiverId;
    private BigDecimal amount;
    private Wallet sender;
    private Wallet receiver;

    @BeforeEach
    void setUp() {
        transferId = "31221L";
        senderId = "1L";
        receiverId = "2L";
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
                    "323291L", senderId, receiverId, amount, LocalDateTime.now()));
            when(transferMapper.toResponse(any())).thenReturn(new TransferDTO.Response(
                    "323291L", senderId, receiverId, amount, LocalDateTime.now()));

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
            var expectedResponse = new TransferDTO.Response(transferId, senderId, receiverId, amount, LocalDateTime.now());

            when(transferRepository.findById(transferId)).thenReturn(Optional.of(new Transfer()));
            when(transferMapper.toResponse(any())).thenReturn(expectedResponse);

            var response = transferService.findById(transferId);

            assertEquals(expectedResponse, response);
            verify(transferRepository).findById(transferId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when transfer not found")
        void shouldThrowResourceNotFoundExceptionWhenTransferNotFound() {

            when(transferRepository.findById(transferId)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class, () -> transferService.findById(transferId)
            );

            assertTrue(exception.getMessage().contains("Transfer not found"));
            verify(transferMapper, never()).toResponse(any());
        }
    }

    @Nested
    class FindAllTransfers {
        @Test
        @DisplayName("Should return paginated transfers")
        void shouldReturnPaginatedTransfers() {
            List<Transfer> transfers = List.of(
                new Transfer(transferId, senderId, receiverId, amount, LocalDateTime.now()),
                new Transfer("12345L", senderId, receiverId, amount, LocalDateTime.now())
            );
            
            Page<Transfer> transferPage = new PageImpl<>(transfers);
            
            when(transferRepository.findAll(any(Pageable.class))).thenReturn(transferPage);
            
            var result = transferService.findAll(mock(Pageable.class));
            
            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            verify(transferRepository).findAll(any(Pageable.class));
        }
        
        @Test
        @DisplayName("Should return empty page when no transfers exist")
        void shouldReturnEmptyPageWhenNoTransfersExist() {
            Page<Transfer> emptyPage = new PageImpl<>(List.of());
            
            when(transferRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);
            
            var result = transferService.findAll(mock(Pageable.class));
            
            assertNotNull(result);
            assertEquals(0, result.getContent().size());
            verify(transferRepository).findAll(any(Pageable.class));
        }
    }

    @Nested
    class FindTransfersBySender {
        @Test
        @DisplayName("Should return transfers by sender ID")
        void shouldReturnTransfersBySenderId() {
            List<Transfer> transfers = List.of(
                new Transfer(transferId, senderId, receiverId, amount, LocalDateTime.now()),
                new Transfer("12345L", senderId, receiverId, amount, LocalDateTime.now())
            );
            
            when(transferRepository.findTransferBySenderId(senderId)).thenReturn(transfers);
            when(transferMapper.toResponse(any())).thenReturn(
                new TransferDTO.Response(transferId, senderId, receiverId, amount, LocalDateTime.now()),
                new TransferDTO.Response("12345L", senderId, receiverId, amount, LocalDateTime.now())
            );
            
            var result = transferService.findBySender(senderId);
            
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(transferRepository).findTransferBySenderId(senderId);
            verify(transferMapper, times(2)).toResponse(any());
        }
        
        @Test
        @DisplayName("Should return empty list when sender has no transfers")
        void shouldReturnEmptyListWhenSenderHasNoTransfers() {
            when(transferRepository.findTransferBySenderId(senderId)).thenReturn(List.of());
            
            var result = transferService.findBySender(senderId);
            
            assertNotNull(result);
            assertEquals(0, result.size());
            verify(transferRepository).findTransferBySenderId(senderId);
            verify(transferMapper, never()).toResponse(any());
        }
    }

    @Nested
    class FindTransfersByReceiver {
        @Test
        @DisplayName("Should return transfers by receiver ID")
        void shouldReturnTransfersByReceiverId() {
            List<Transfer> transfers = List.of(
                    new Transfer(transferId, senderId, receiverId, amount, LocalDateTime.now()),
                    new Transfer("12345L", senderId, receiverId, amount, LocalDateTime.now())
            );

            when(transferRepository.findTransferByReceiverId(senderId)).thenReturn(transfers);
            when(transferMapper.toResponse(any())).thenReturn(
                    new TransferDTO.Response(transferId, senderId, receiverId, amount, LocalDateTime.now()),
                    new TransferDTO.Response("12345L", senderId, receiverId, amount, LocalDateTime.now())
            );

            var result = transferService.findByReceiver(senderId);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(transferRepository).findTransferByReceiverId(senderId);
            verify(transferMapper, times(2)).toResponse(any());
        }

        @Test
        @DisplayName("Should return empty list when receiver has no transfers")
        void shouldReturnEmptyListWhenReceiverHasNoTransfers() {
            when(transferRepository.findTransferByReceiverId(senderId)).thenReturn(List.of());

            var result = transferService.findByReceiver(senderId);

            assertNotNull(result);
            assertEquals(0, result.size());
            verify(transferRepository).findTransferByReceiverId(senderId);
            verify(transferMapper, never()).toResponse(any());
        }
    }
}
