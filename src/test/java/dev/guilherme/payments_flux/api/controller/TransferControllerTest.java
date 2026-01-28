package dev.guilherme.payments_flux.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.api.exception.BusinessException;
import dev.guilherme.payments_flux.api.exception.ResourceNotFoundException;
import dev.guilherme.payments_flux.domain.service.transfer.TransferService;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    private ObjectMapper objectMapper;

    private TransferDTO.CreateRequest validCreateRequest;
    private TransferDTO.Response transferResponse;
    private UUID transferId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        transferId = UUID.randomUUID();
        validCreateRequest = new TransferDTO.CreateRequest(
            1L,
            2L,
            BigDecimal.valueOf(100.50)
        );

        transferResponse = new TransferDTO.Response(
            transferId,
            1L,
            2L,
            BigDecimal.valueOf(100.50),
            LocalDateTime.now()
        );
    }

    @Nested
    class CreateTransfer {
        @Test
        @DisplayName("Should create transfer successfully and return 201")
        void shouldCreateTransferSuccessfullyAndReturn201() throws Exception {
            when(transferService.create(any(TransferDTO.CreateRequest.class)))
                .thenReturn(transferResponse);

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(transferId.toString()))
                    .andExpect(jsonPath("$.senderId").value(1))
                    .andExpect(jsonPath("$.receiverId").value(2))
                    .andExpect(jsonPath("$.amount").value(100.50))
                    .andExpect(jsonPath("$.createdAt").exists());

            verify(transferService, times(1)).create(any(TransferDTO.CreateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when creating transfer with invalid data")
        void shouldReturn400WhenCreatingTransferWithInvalidData() throws Exception {
            TransferDTO.CreateRequest invalidRequest = new TransferDTO.CreateRequest(
                null,
                2L,
                BigDecimal.valueOf(-100)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when creating transfer with zero amount")
        void shouldReturn400WhenCreatingTransferWithZeroAmount() throws Exception {
            TransferDTO.CreateRequest zeroAmountRequest = new TransferDTO.CreateRequest(
                1L,
                2L,
                BigDecimal.ZERO
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(zeroAmountRequest)))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when JSON is malformed")
        void shouldReturn400WhenJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json}"))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).create(any());
        }
    }

    @Nested
    class FindTransferById {
        @Test
        @DisplayName("Should find transfer by ID successfully and return 200")
        void shouldFindTransferByIdSuccessfullyAndReturn200() throws Exception {
            when(transferService.findById(transferId)).thenReturn(transferResponse);

            mockMvc.perform(get("/v1/api/transfer/find/{id}", transferId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(transferId.toString()))
                    .andExpect(jsonPath("$.senderId").value(1))
                    .andExpect(jsonPath("$.receiverId").value(2))
                    .andExpect(jsonPath("$.amount").value(100.50))
                    .andExpect(jsonPath("$.createdAt").exists());

            verify(transferService, times(1)).findById(transferId);
        }

        @Test
        @DisplayName("Should return 404 when transfer not found")
        void shouldReturn404WhenTransferNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(transferService.findById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Transfer not found", transferId));

            mockMvc.perform(get("/v1/api/transfer/find/{id}", nonExistentId))
                    .andExpect(status().isNotFound());

            verify(transferService, times(1)).findById(nonExistentId);
        }

        @Test
        @DisplayName("Should return 400 when ID is invalid UUID format")
        void shouldReturn400WhenIdIsInvalidUuidFormat() throws Exception {
            mockMvc.perform(get("/v1/api/transfer/find/{id}", "invalid-uuid"))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).findById(any());
        }
    }

    @Nested
    class BusinessLogicTests {
        @Test
        @DisplayName("Should return 400 when transferring to same wallet")
        void shouldReturn400WhenTransferringToSameWallet() throws Exception {
            TransferDTO.CreateRequest sameWalletRequest = new TransferDTO.CreateRequest(
                1L,
                1L,
                BigDecimal.valueOf(100.00)
            );

            when(transferService.create(any(TransferDTO.CreateRequest.class)))
                .thenThrow(new BusinessException("Cannot transfer to same wallet"));

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sameWalletRequest)))
                    .andExpect(status().isBadRequest());

            verify(transferService, times(1)).create(any(TransferDTO.CreateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when insufficient balance")
        void shouldReturn400WhenInsufficientBalance() throws Exception {
            when(transferService.create(any(TransferDTO.CreateRequest.class)))
                .thenThrow(new BusinessException("Insufficient balance"));

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isBadRequest());

            verify(transferService, times(1)).create(any(TransferDTO.CreateRequest.class));
        }

        @Test
        @DisplayName("Should return 404 when sender wallet not found")
        void shouldReturn404WhenSenderWalletNotFound() throws Exception {
            when(transferService.create(any(TransferDTO.CreateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Sender wallet not found", transferId));

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isNotFound());

            verify(transferService, times(1)).create(any(TransferDTO.CreateRequest.class));
        }

        @Test
        @DisplayName("Should return 404 when receiver wallet not found")
        void shouldReturn404WhenReceiverWalletNotFound() throws Exception {
            when(transferService.create(any(TransferDTO.CreateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Receiver wallet not found", transferId));

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isNotFound());

            verify(transferService, times(1)).create(any(TransferDTO.CreateRequest.class));
        }
    }

    @Nested
    class ValidationTests {
        @Test
        @DisplayName("Should validate positive amount")
        void shouldValidatePositiveAmount() throws Exception {
            TransferDTO.CreateRequest negativeAmountRequest = new TransferDTO.CreateRequest(
                1L,
                2L,
                BigDecimal.valueOf(-50.00)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(negativeAmountRequest)))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).create(any());
        }

        @Test
        @DisplayName("Should validate non-null sender ID")
        void shouldValidateNonNullSenderId() throws Exception {
            TransferDTO.CreateRequest nullSenderRequest = new TransferDTO.CreateRequest(
                null,
                2L,
                BigDecimal.valueOf(100.00)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(nullSenderRequest)))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).create(any());
        }

        @Test
        @DisplayName("Should validate non-null receiver ID")
        void shouldValidateNonNullReceiverId() throws Exception {
            TransferDTO.CreateRequest nullReceiverRequest = new TransferDTO.CreateRequest(
                1L,
                null,
                BigDecimal.valueOf(100.00)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(nullReceiverRequest)))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).create(any());
        }

        @Test
        @DisplayName("Should validate amount precision")
        void shouldValidateAmountPrecision() throws Exception {
            TransferDTO.CreateRequest highPrecisionRequest = new TransferDTO.CreateRequest(
                1L,
                2L,
                BigDecimal.valueOf(100.123456789)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(highPrecisionRequest)))
                    .andExpect(status().isBadRequest());

            verify(transferService, never()).create(any());
        }
    }
}
