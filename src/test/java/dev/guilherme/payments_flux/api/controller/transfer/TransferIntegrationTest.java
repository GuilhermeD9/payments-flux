package dev.guilherme.payments_flux.api.controller.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.domain.entity.Transfer;
import dev.guilherme.payments_flux.domain.entity.Wallet;
import dev.guilherme.payments_flux.domain.repository.TransferRepository;
import dev.guilherme.payments_flux.domain.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransferIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transferRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Wallet sender;
    private Wallet receiver;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sender = new Wallet();
        sender.setFullName("Sender User");
        sender.setCpfCnpj("16244749006");
        sender.setEmail("sender@email.com");
        sender.setPassword("password123");
        sender.setBalance(BigDecimal.valueOf(1000.00));
        sender = walletRepository.save(sender);

        receiver = new Wallet();
        receiver.setFullName("Receiver User");
        receiver.setCpfCnpj("53359657039");
        receiver.setEmail("receiver@email.com");
        receiver.setPassword("password123");
        receiver.setBalance(BigDecimal.valueOf(500.00));
        receiver = walletRepository.save(receiver);
    }

    @Nested
    @DisplayName("Create Transfer Integration Tests")
    class CreateTransferTests {
        @Test
        @DisplayName("Should create transfer successfully and update balances")
        void shouldCreateTransferSuccessfullyAndUpdateBalances() throws Exception {
            TransferDTO.CreateRequest request = new TransferDTO.CreateRequest(
                sender.getId(),
                receiver.getId(),
                BigDecimal.valueOf(200.00)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.senderId").value(sender.getId()))
                    .andExpect(jsonPath("$.receiverId").value(receiver.getId()))
                    .andExpect(jsonPath("$.amount").value(200.00))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.createdAt").exists());

            Wallet updatedSender = walletRepository.findById(sender.getId()).orElseThrow();
            Wallet updatedReceiver = walletRepository.findById(receiver.getId()).orElseThrow();

            assertEquals(BigDecimal.valueOf(800.00), updatedSender.getBalance());
            assertEquals(BigDecimal.valueOf(700.00), updatedReceiver.getBalance());

            assertEquals(1, transferRepository.count());
        }

        @Test
        @DisplayName("Should return 400 when transferring to same wallet")
        void shouldReturn400WhenTransferringToSameWallet() throws Exception {
            TransferDTO.CreateRequest request = new TransferDTO.CreateRequest(
                sender.getId(),
                sender.getId(),
                BigDecimal.valueOf(100.00)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertEquals(0, transferRepository.count());
        }

        @Test
        @DisplayName("Should return 400 when insufficient balance")
        void shouldReturn400WhenInsufficientBalance() throws Exception {
            TransferDTO.CreateRequest request = new TransferDTO.CreateRequest(
                sender.getId(),
                receiver.getId(),
                BigDecimal.valueOf(2000.00)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertEquals(0, transferRepository.count());
        }

        @Test
        @DisplayName("Should return 404 when sender wallet not found")
        void shouldReturn404WhenSenderWalletNotFound() throws Exception {
            TransferDTO.CreateRequest request = new TransferDTO.CreateRequest(
                999L,
                receiver.getId(),
                BigDecimal.valueOf(100.00)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            assertEquals(0, transferRepository.count());
        }

        @Test
        @DisplayName("Should return 404 when receiver wallet not found")
        void shouldReturn404WhenReceiverWalletNotFound() throws Exception {
            TransferDTO.CreateRequest request = new TransferDTO.CreateRequest(
                sender.getId(),
                999L,
                BigDecimal.valueOf(100.00)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            assertEquals(0, transferRepository.count());
        }
    }

    @Nested
    @DisplayName("Find Transfer By ID Integration Tests")
    class FindTransferByIdTests {
        @Test
        @DisplayName("Should find transfer by ID successfully")
        void shouldFindTransferByIdSuccessfully() throws Exception {
            Transfer transfer = new Transfer();
            transfer.setSender(sender);
            transfer.setReceiver(receiver);
            transfer.setAmount(BigDecimal.valueOf(100.00));
            transfer.setCreatedAt(LocalDateTime.now());
            transfer = transferRepository.save(transfer);

            mockMvc.perform(get("/v1/api/transfer/find/{id}", transfer.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(transfer.getId().toString()))
                    .andExpect(jsonPath("$.senderId").value(sender.getId()))
                    .andExpect(jsonPath("$.receiverId").value(receiver.getId()))
                    .andExpect(jsonPath("$.amount").value(100.00))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("Should return 404 when transfer not found")
        void shouldReturn404WhenTransferNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/v1/api/transfer/find/{id}", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Validation Integration Tests")
    class ValidationTests {
        @Test
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() throws Exception {
            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());

            assertEquals(0, transferRepository.count());
        }

        @Test
        @DisplayName("Should validate positive amount")
        void shouldValidatePositiveAmount() throws Exception {
            TransferDTO.CreateRequest request = new TransferDTO.CreateRequest(
                sender.getId(),
                receiver.getId(),
                BigDecimal.valueOf(-100.00)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertEquals(0, transferRepository.count());
        }

        @Test
        @DisplayName("Should validate amount precision")
        void shouldValidateAmountPrecision() throws Exception {
            TransferDTO.CreateRequest request = new TransferDTO.CreateRequest(
                sender.getId(),
                receiver.getId(),
                BigDecimal.valueOf(100.123456789)
            );

            mockMvc.perform(post("/v1/api/transfer/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertEquals(0, transferRepository.count());
        }
    }
}
