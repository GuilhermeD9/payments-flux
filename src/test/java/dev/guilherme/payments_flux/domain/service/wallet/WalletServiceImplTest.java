package dev.guilherme.payments_flux.domain.service.wallet;

import dev.guilherme.payments_flux.api.dto.WalletDTO;
import dev.guilherme.payments_flux.api.exception.ResourceNotFoundException;
import dev.guilherme.payments_flux.api.mapper.WalletMapper;
import dev.guilherme.payments_flux.domain.entity.Wallet;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private WalletServiceImpl walletService;

    Long walletId;
    String fullName;
    String cpfCnpj;
    String email;
    String password;
    BigDecimal balance;
    Long version;

    @BeforeEach
    void setUp() {
        walletId = 321L;
        fullName = "Marcelo Silvano";
        cpfCnpj = "21704662079";
        email = "quialqiuremail@email.com";
        password = "123100";
        balance = BigDecimal.TEN;
        version = 1L;
    }

    @Nested
    class CreateWallet {
        @Test
        @DisplayName("Should create wallet with valid data and return response")
        void shouldCreateWalletWithValidDataAndReturnResponse() {
            var requestDTO = new WalletDTO.CreateRequest(fullName, cpfCnpj, email, password);

            when(walletMapper.toEntity(any())).thenReturn(new Wallet());
            when(walletRepository.save(any())).thenReturn(new Wallet(
                    walletId, fullName, cpfCnpj, email, "212312", BigDecimal.ZERO, version));
            when(walletMapper.toResponse(any())).thenReturn(new WalletDTO.Response(
                    walletId, fullName, cpfCnpj, email, BigDecimal.ZERO));

            var response = walletService.create(requestDTO);

            assertNotNull(response);
            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());
            Wallet capturedWallet = walletCaptor.getValue();
            assertEquals(BigDecimal.ZERO, capturedWallet.getBalance());
            verify(passwordEncoder).encode("123100");
        }

        @Test
        @DisplayName("Should encode password when creating wallet")
        void shouldEncodePasswordWhenCreatingWallet() {
            var requestDTO = new WalletDTO.CreateRequest(fullName, cpfCnpj, email, password);

            when(walletMapper.toEntity(any())).thenReturn(new Wallet());
            when(walletRepository.save(any())).thenReturn(new Wallet(walletId, fullName, cpfCnpj, email, "encoded", BigDecimal.ZERO, version));
            when(walletMapper.toResponse(any())).thenReturn(new WalletDTO.Response(walletId, fullName, cpfCnpj, email, BigDecimal.ZERO));
            when(passwordEncoder.encode(password)).thenReturn("encoded");

            walletService.create(requestDTO);

            verify(passwordEncoder).encode(password);
        }
    }

    @Nested
    class FindWalletById {
        @Test
        @DisplayName("Should find wallet by valid ID and return response")
        void shouldFindWalletByValidIdAndReturnResponse() {
            Wallet wallet = new Wallet(walletId, fullName, cpfCnpj, email, password, balance, version);

            WalletDTO.Response expectedResponse = new WalletDTO.Response(
                    walletId, fullName, cpfCnpj, email, balance
            );

            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
            when(walletMapper.toResponse(wallet)).thenReturn(expectedResponse);

            WalletDTO.Response response = walletService.findById(walletId);

            assertNotNull(response);
            assertEquals(expectedResponse.id(), response.id());
            verify(walletRepository).findById(walletId);
            verify(walletMapper).toResponse(wallet);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when wallet not found")
        void shouldThrowResourceNotFoundExcpetionWhenWalletNotFound() {
            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class, () -> walletService.findById(walletId)
            );

            assertTrue(exception.getMessage().contains("Wallet not found"));
            verify(walletMapper, never()).toResponse(any());
        }
    }

    @Nested
    class UpdateWallet {
        @Test
        @DisplayName("Should update wallet with valid data and return response")
        void shouldUpdateWalletWithValidDataAndReturnResponse() {
            WalletDTO.UpdateRequest updateRequest = new WalletDTO.UpdateRequest(
                    "Updated Name", cpfCnpj, "updated@email.com", password, balance);

            Wallet existingWallet = new Wallet(
                    walletId, fullName, cpfCnpj, email, password, balance, version);
            Wallet updatedWallet = new Wallet(
                    walletId, "Updated Name", cpfCnpj, "updated@email.com", password, balance, version);

            WalletDTO.Response expectedResponse = new WalletDTO.Response(
                    walletId, "Updated Name", cpfCnpj, "updated@email.com", balance
            );

            when(walletRepository.findById(walletId)).thenReturn(Optional.of(existingWallet));
            when(walletRepository.save(any())).thenReturn(updatedWallet);
            when(walletMapper.toResponse(updatedWallet)).thenReturn(expectedResponse);

            WalletDTO.Response response = walletService.update(walletId, updateRequest);

            assertNotNull(response);
            assertEquals("Updated Name", response.fullName());
            assertEquals("updated@email.com", response.email());
            verify(walletRepository).findById(walletId);
            verify(walletRepository).save(existingWallet);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent wallet")
        void shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistentWallet() {
            WalletDTO.UpdateRequest updateRequest = new WalletDTO.UpdateRequest(
                    fullName, cpfCnpj, email, password, balance);

            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> walletService.update(walletId, updateRequest)
            );

            assertTrue(exception.getMessage().contains("Wallet not found"));
            verify(walletRepository, never()).save(any());
            verify(walletMapper, never()).toResponse(any());
        }
    }

    @Nested
    class DeleteWallet {
        @Test
        @DisplayName("Should delete wallet with valid ID successfully")
        void shouldDeleteWalletWithValidIdSuccessfully() {
            when(walletRepository.existsById(walletId)).thenReturn(true);

            assertDoesNotThrow(() -> walletService.delete(walletId));

            verify(walletRepository).existsById(walletId);
            verify(walletRepository).deleteById(walletId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent wallet")
        void shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentWallet() {
            when(walletRepository.existsById(walletId)).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> walletService.delete(walletId)
            );

            assertTrue(exception.getMessage().contains("Wallet not found"));
            verify(walletRepository, never()).deleteById(any());
        }
    }
}