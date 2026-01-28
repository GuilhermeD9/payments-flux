package dev.guilherme.payments_flux.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.guilherme.payments_flux.api.dto.WalletDTO;
import dev.guilherme.payments_flux.api.exception.ResourceNotFoundException;
import dev.guilherme.payments_flux.domain.service.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    private ObjectMapper objectMapper;

    private WalletDTO.CreateRequest validCreateRequest;
    private WalletDTO.UpdateRequest validUpdateRequest;
    private WalletDTO.Response walletResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validCreateRequest = new WalletDTO.CreateRequest(
            "João Silva",
            "12345678909",
            "joao.silva@email.com",
            "senha123"
        );

        validUpdateRequest = new WalletDTO.UpdateRequest(
            "João Silva Updated",
            "12345678909",
                "joao.silva@email.com",
                "senha123",
                BigDecimal.TEN
        );

        walletResponse = new WalletDTO.Response(
            1L,
            "João Silva",
            "12345678909",
            "joao.silva@email.com",
            BigDecimal.ZERO
        );
    }

    @Nested
    class CreateWallet {
        @Test
        @DisplayName("Should create wallet successfully and return 201")
        void shouldCreateWalletSuccessfullyAndReturn201() throws Exception {
            when(walletService.create(any(WalletDTO.CreateRequest.class))).thenReturn(walletResponse);

            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fullName").value("João Silva"))
                    .andExpect(jsonPath("$.cpfCnpj").value("12345678909"))
                    .andExpect(jsonPath("$.email").value("joao.silva@email.com"))
                    .andExpect(jsonPath("$.balance").value(0));

            verify(walletService, times(1)).create(any(WalletDTO.CreateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when creating wallet with invalid data")
        void shouldReturn400WhenCreatingWalletWithInvalidData() throws Exception {
            WalletDTO.CreateRequest invalidRequest = new WalletDTO.CreateRequest(
                "",
                "123",
                "invalid-email",
                ""
            );

            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(walletService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(walletService, never()).create(any());
        }
    }

    @Nested
    class FindWalletById {
        @Test
        @DisplayName("Should find wallet by ID successfully and return 200")
        void shouldFindWalletByIdSuccessfullyAndReturn200() throws Exception {
            when(walletService.findById(1L)).thenReturn(walletResponse);

            mockMvc.perform(get("/v1/api/wallet/find/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fullName").value("João Silva"))
                    .andExpect(jsonPath("$.cpfCnpj").value("12345678909"))
                    .andExpect(jsonPath("$.email").value("joao.silva@email.com"))
                    .andExpect(jsonPath("$.balance").value(0));

            verify(walletService, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should return 404 when wallet not found")
        void shouldReturn404WhenWalletNotFound() throws Exception {
            when(walletService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Wallet not found", 999L));

            mockMvc.perform(get("/v1/api/wallet/find/{id}", 999L))
                    .andExpect(status().isNotFound());

            verify(walletService, times(1)).findById(999L);
        }
    }

    @Nested
    class UpdateWallet {
        @Test
        @DisplayName("Should update wallet successfully and return 200")
        void shouldUpdateWalletSuccessfullyAndReturn200() throws Exception {
            WalletDTO.Response updatedResponse = new WalletDTO.Response(
                1L,
                "João Silva Updated",
                "12636874070",
                "joao.updated@email.com",
                BigDecimal.ONE
            );

            when(walletService.update(eq(1L), any(WalletDTO.UpdateRequest.class)))
                .thenReturn(updatedResponse);

            mockMvc.perform(put("/v1/api/wallet/update/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fullName").value("João Silva Updated"))
                    .andExpect(jsonPath("$.email").value("joao.updated@email.com"))
                    .andExpect(jsonPath("$.balance").value("1"));

            verify(walletService, times(1)).update(eq(1L), any(WalletDTO.UpdateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when updating wallet with invalid data")
        void shouldReturn400WhenUpdatingWalletWithInvalidData() throws Exception {
            WalletDTO.UpdateRequest invalidRequest = new WalletDTO.UpdateRequest(
                "",
                "invalid-email@a",
                    "cepeefe",
                    "japa1",
                    BigDecimal.TEN
            );

            mockMvc.perform(put("/v1/api/wallet/update/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(walletService, never()).update(anyLong(), any());
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent wallet")
        void shouldReturn404WhenUpdatingNonExistentWallet() throws Exception {
            when(walletService.update(eq(999L), any(WalletDTO.UpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Wallet not found", 999L));

            mockMvc.perform(put("/v1/api/wallet/update/{id}", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isNotFound());

            verify(walletService, times(1)).update(eq(999L), any(WalletDTO.UpdateRequest.class));
        }
    }

    @Nested
    class DeleteWallet {
        @Test
        @DisplayName("Should delete wallet successfully and return 204")
        void shouldDeleteWalletSuccessfullyAndReturn204() throws Exception {
            mockMvc.perform(delete("/v1/api/wallet/delete/{id}", 1L))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(walletService, times(1)).delete(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent wallet")
        void shouldReturn404WhenDeletingNonExistentWallet() throws Exception {
            doThrow(new ResourceNotFoundException("Wallet not found", 999L))
                .when(walletService).delete(999L);

            mockMvc.perform(delete("/v1/api/wallet/delete/{id}", 999L))
                    .andExpect(status().isNotFound());

            verify(walletService, times(1)).delete(999L);
        }
    }

    @Nested
    class ValidationTests {
        @Test
        @DisplayName("Should validate CPF/CNPJ format")
        void shouldValidateCpfCnpjFormat() throws Exception {
            WalletDTO.CreateRequest requestWithInvalidCpf = new WalletDTO.CreateRequest(
                "João Silva",
                "111.111.111-11",
                "joao.silva@email.com",
                "senha123"
            );

            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestWithInvalidCpf)))
                    .andExpect(status().isBadRequest());

            verify(walletService, never()).create(any());
        }

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() throws Exception {
            WalletDTO.CreateRequest requestWithInvalidEmail = new WalletDTO.CreateRequest(
                "João Silva",
                "12345678909",
                "not-an-email",
                "senha123"
            );

            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestWithInvalidEmail)))
                    .andExpect(status().isBadRequest());

            verify(walletService, never()).create(any());
        }

        @Test
        @DisplayName("Should validate password minimum length")
        void shouldValidatePasswordMinimumLength() throws Exception {
            WalletDTO.CreateRequest requestWithShortPassword = new WalletDTO.CreateRequest(
                "João Silva",
                "12345678909",
                "joao.silva@email.com",
                "123"
            );

            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestWithShortPassword)))
                    .andExpect(status().isBadRequest());

            verify(walletService, never()).create(any());
        }
    }
}
