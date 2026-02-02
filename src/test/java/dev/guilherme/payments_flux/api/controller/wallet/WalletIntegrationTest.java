package dev.guilherme.payments_flux.api.controller.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.guilherme.payments_flux.api.dto.WalletDTO;
import dev.guilherme.payments_flux.domain.entity.Wallet;
import dev.guilherme.payments_flux.domain.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class WalletIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private WalletRepository walletRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    class CreateWalletTests {
        @Test
        @DisplayName("Should create wallet successfully")
        void shouldCreateWalletSuccessfully() throws Exception {
            WalletDTO.CreateRequest request = new WalletDTO.CreateRequest(
                "John Doe",
                "150.846.050-78",
                "john.doe@email.com",
                "password123"
            );

            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.fullName").value("John Doe"))
                    .andExpect(jsonPath("$.cpfCnpj").value("15084605078"))
                    .andExpect(jsonPath("$.email").value("john.doe@email.com"))
                    .andExpect(jsonPath("$.balance").value(0));

            assertEquals(1, walletRepository.count());
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

            assertEquals(0, walletRepository.count());
        }

        @Test
        @DisplayName("Should validate CPF/CNPJ format")
        void shouldValidateCpfCnpjFormat() throws Exception {
            WalletDTO.CreateRequest request = new WalletDTO.CreateRequest(
                "John Doe",
                "111.111.111-11",
                "john.doe@email.com",
                "password123"
            );

            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertEquals(0, walletRepository.count());
        }

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() throws Exception {
            WalletDTO.CreateRequest request = new WalletDTO.CreateRequest(
                "John Doe",
                "150.846.050-78",
                "not-an-email",
                "password123"
            );

            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertEquals(0, walletRepository.count());
        }

        @Test
        @DisplayName("Should validate password minimum length")
        void shouldValidatePasswordMinimumLength() throws Exception {
            WalletDTO.CreateRequest request = new WalletDTO.CreateRequest(
                "John Doe",
                "150.846.050-78",
                "john.doe@email.com",
                "123"
            );

            mockMvc.perform(post("/v1/api/wallet/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertEquals(0, walletRepository.count());
        }
    }

    @Nested
    class FindWalletByIdTests {
        @Test
        @DisplayName("Should find wallet by ID successfully")
        void shouldFindWalletByIdSuccessfully() throws Exception {
            Wallet wallet = new Wallet();
            wallet.setFullName("John Doe");
            wallet.setCpfCnpj("150.846.050-78");
            wallet.setEmail("john.doe@email.com");
            wallet.setPassword("password123");
            wallet.setBalance(BigDecimal.valueOf(100.00));
            wallet = walletRepository.save(wallet);

            mockMvc.perform(get("/v1/api/wallet/find/{id}", wallet.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(wallet.getId()))
                    .andExpect(jsonPath("$.fullName").value("John Doe"))
                    .andExpect(jsonPath("$.cpfCnpj").value("15084605078"))
                    .andExpect(jsonPath("$.email").value("john.doe@email.com"))
                    .andExpect(jsonPath("$.balance").value(100.00));
        }

        @Test
        @DisplayName("Should return 404 when wallet not found")
        void shouldReturn404WhenWalletNotFound() throws Exception {
            mockMvc.perform(get("/v1/api/wallet/find/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Update Wallet Integration Tests")
    class UpdateWalletTests {
        @Test
        @DisplayName("Should update wallet successfully")
        void shouldUpdateWalletSuccessfully() throws Exception {
            Wallet wallet = new Wallet();
            wallet.setFullName("John Doe");
            wallet.setCpfCnpj("150.846.050-78");
            wallet.setEmail("john.doe@email.com");
            wallet.setPassword("password123");
            wallet.setBalance(BigDecimal.valueOf(100.00));
            wallet = walletRepository.save(wallet);

            WalletDTO.UpdateRequest updateRequest = new WalletDTO.UpdateRequest(
                "John Updated",
                "03033419046",
                "john.updated@email.com",
                "newpassword123"
            );

            mockMvc.perform(put("/v1/api/wallet/update/{id}", wallet.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(wallet.getId()))
                    .andExpect(jsonPath("$.fullName").value("John Updated"))
                    .andExpect(jsonPath("$.email").value("john.updated@email.com"))
                    .andExpect(jsonPath("$.balance").value(100.00));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent wallet")
        void shouldReturn404WhenUpdatingNonExistentWallet() throws Exception {
            WalletDTO.UpdateRequest updateRequest = new WalletDTO.UpdateRequest(
                    "John Updated",
                    "03033419046",
                    "john.updated@email.com",
                    "newpassword123"
            );

            mockMvc.perform(put("/v1/api/wallet/update/{id}", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when updating wallet with invalid data")
        void shouldReturn400WhenUpdatingWalletWithInvalidData() throws Exception {
            Wallet wallet = new Wallet();
            wallet.setFullName("John Doe");
            wallet.setCpfCnpj("150.846.050-78");
            wallet.setEmail("john.doe@email.com");
            wallet.setPassword("password123");
            wallet.setBalance(BigDecimal.valueOf(100.00));
            wallet = walletRepository.save(wallet);

            WalletDTO.UpdateRequest invalidRequest = new WalletDTO.UpdateRequest(
                "",
                "invalid-email",
                "123",
                ""
            );

            mockMvc.perform(put("/v1/api/wallet/update/{id}", wallet.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Delete Wallet Integration Tests")
    class DeleteWalletTests {
        @Test
        @DisplayName("Should delete wallet successfully")
        void shouldDeleteWalletSuccessfully() throws Exception {
            Wallet wallet = new Wallet();
            wallet.setFullName("John Doe");
            wallet.setCpfCnpj("150.846.050-78");
            wallet.setEmail("john.doe@email.com");
            wallet.setPassword("password123");
            wallet.setBalance(BigDecimal.valueOf(100.00));
            wallet = walletRepository.save(wallet);

            mockMvc.perform(delete("/v1/api/wallet/delete/{id}", wallet.getId()))
                    .andExpect(status().isNoContent());

            assertEquals(0, walletRepository.count());
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent wallet")
        void shouldReturn404WhenDeletingNonExistentWallet() throws Exception {
            mockMvc.perform(delete("/v1/api/wallet/delete/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Find All Wallets Integration Tests")
    class FindAllWalletsTests {
        @Test
        @DisplayName("Should return all wallets successfully")
        void shouldReturnAllWalletsSuccessfully() throws Exception {
            Wallet wallet1 = new Wallet();
            wallet1.setFullName("John Doe");
            wallet1.setCpfCnpj("150.846.050-78");
            wallet1.setEmail("john.doe@email.com");
            wallet1.setPassword("password123");
            wallet1.setBalance(BigDecimal.valueOf(100.00));
            walletRepository.save(wallet1);

            Wallet wallet2 = new Wallet();
            wallet2.setFullName("Jane Smith");
            wallet2.setCpfCnpj("987.654.321-00");
            wallet2.setEmail("jane.smith@email.com");
            wallet2.setPassword("password123");
            wallet2.setBalance(BigDecimal.valueOf(200.00));
            walletRepository.save(wallet2);

            mockMvc.perform(get("/v1/api/wallet/findAll"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].fullName").exists())
                    .andExpect(jsonPath("$[1].fullName").exists());
        }

        @Test
        @DisplayName("Should return empty list when no wallets exist")
        void shouldReturnEmptyListWhenNoWalletsExist() throws Exception {
            mockMvc.perform(get("/v1/api/wallet/findAll"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Deposit Money Integration Tests")
    class DepositMoneyTests {
        @Test
        @DisplayName("Should deposit money successfully")
        void shouldDepositMoneySuccessfully() throws Exception {
            Wallet wallet = new Wallet();
            wallet.setFullName("John Doe");
            wallet.setCpfCnpj("150.846.050-78");
            wallet.setEmail("john.doe@email.com");
            wallet.setPassword("password123");
            wallet.setBalance(BigDecimal.valueOf(100.00));
            wallet = walletRepository.save(wallet);

            WalletDTO.MoneyRequest depositRequest = new WalletDTO.MoneyRequest(
                BigDecimal.valueOf(50.00)
            );

            mockMvc.perform(post("/v1/api/wallet/deposit/{id}", wallet.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(depositRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(wallet.getId()))
                    .andExpect(jsonPath("$.balance").value(150.00));

            Wallet updatedWallet = walletRepository.findById(wallet.getId()).orElseThrow();
            assertEquals(BigDecimal.valueOf(150.00), updatedWallet.getBalance());
        }

        @Test
        @DisplayName("Should return 404 when depositing to non-existent wallet")
        void shouldReturn404WhenDepositingToNonExistentWallet() throws Exception {
            WalletDTO.MoneyRequest depositRequest = new WalletDTO.MoneyRequest(
                BigDecimal.valueOf(50.00)
            );

            mockMvc.perform(post("/v1/api/wallet/deposit/{id}", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(depositRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when depositing negative amount")
        void shouldReturn400WhenDepositingNegativeAmount() throws Exception {
            Wallet wallet = new Wallet();
            wallet.setFullName("John Doe");
            wallet.setCpfCnpj("150.846.050-78");
            wallet.setEmail("john.doe@email.com");
            wallet.setPassword("password123");
            wallet.setBalance(BigDecimal.valueOf(100.00));
            wallet = walletRepository.save(wallet);

            WalletDTO.MoneyRequest depositRequest = new WalletDTO.MoneyRequest(
                BigDecimal.valueOf(-50.00)
            );

            mockMvc.perform(post("/v1/api/wallet/deposit/{id}", wallet.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(depositRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Withdraw Money Integration Tests")
    class WithdrawMoneyTests {
        @Test
        @DisplayName("Should withdraw money successfully")
        void shouldWithdrawMoneySuccessfully() throws Exception {
            Wallet wallet = new Wallet();
            wallet.setFullName("John Doe");
            wallet.setCpfCnpj("150.846.050-78");
            wallet.setEmail("john.doe@email.com");
            wallet.setPassword("password123");
            wallet.setBalance(BigDecimal.valueOf(100.00));
            wallet = walletRepository.save(wallet);

            WalletDTO.MoneyRequest withdrawRequest = new WalletDTO.MoneyRequest(
                BigDecimal.valueOf(30.00)
            );

            mockMvc.perform(post("/v1/api/wallet/withdraw/{id}", wallet.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(withdrawRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(wallet.getId()))
                    .andExpect(jsonPath("$.balance").value(70.00));

            Wallet updatedWallet = walletRepository.findById(wallet.getId()).orElseThrow();
            assertEquals(BigDecimal.valueOf(70.00), updatedWallet.getBalance());
        }

        @Test
        @DisplayName("Should return 400 when insufficient balance")
        void shouldReturn400WhenInsufficientBalance() throws Exception {
            Wallet wallet = new Wallet();
            wallet.setFullName("John Doe");
            wallet.setCpfCnpj("150.846.050-78");
            wallet.setEmail("john.doe@email.com");
            wallet.setPassword("password123");
            wallet.setBalance(BigDecimal.valueOf(100.00));
            wallet = walletRepository.save(wallet);

            WalletDTO.MoneyRequest withdrawRequest = new WalletDTO.MoneyRequest(
                BigDecimal.valueOf(150.00)
            );

            mockMvc.perform(post("/v1/api/wallet/withdraw/{id}", wallet.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(withdrawRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when withdrawing from non-existent wallet")
        void shouldReturn404WhenWithdrawingFromNonExistentWallet() throws Exception {
            WalletDTO.MoneyRequest withdrawRequest = new WalletDTO.MoneyRequest(
                BigDecimal.valueOf(50.00)
            );

            mockMvc.perform(post("/v1/api/wallet/withdraw/{id}", 999L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(withdrawRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when withdrawing negative amount")
        void shouldReturn400WhenWithdrawingNegativeAmount() throws Exception {
            Wallet wallet = new Wallet();
            wallet.setFullName("John Doe");
            wallet.setCpfCnpj("150.846.050-78");
            wallet.setEmail("john.doe@email.com");
            wallet.setPassword("password123");
            wallet.setBalance(BigDecimal.valueOf(100.00));
            wallet = walletRepository.save(wallet);

            WalletDTO.MoneyRequest withdrawRequest = new WalletDTO.MoneyRequest(
                BigDecimal.valueOf(-50.00)
            );

            mockMvc.perform(post("/v1/api/wallet/withdraw/{id}", wallet.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(withdrawRequest)))
                    .andExpect(status().isBadRequest());
        }
    }
}
