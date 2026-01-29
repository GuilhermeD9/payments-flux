package dev.guilherme.payments_flux.api.controller.transfer;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.domain.entity.Wallet;
import dev.guilherme.payments_flux.domain.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TransferConcurrencyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long senderId;
    private Long receiverId;

    @BeforeEach
    void setup() {
        walletRepository.deleteAll();

        Wallet sender = new Wallet();
        sender.setEmail("sendertest@email.com");
        sender.setCpfCnpj("23453212322");
        sender.setPassword("abobora");
        sender.setBalance(BigDecimal.valueOf(100.00));
        sender = walletRepository.save(sender);
        this.senderId = sender.getId();

        Wallet receiver = new Wallet();
        receiver.setEmail("receivertest@email.com");
        receiver.setCpfCnpj("23453212321");
        receiver.setPassword("passo");
        receiver.setBalance(BigDecimal.ZERO);
        receiver = walletRepository.save(receiver);
        this.receiverId = receiver.getId();
    }

    @Test
    @DisplayName("Should return 400 when try to create two transfers with the same wallet")
    void shouldHandleConcurrentTransfers() throws Exception {
        TransferDTO.CreateRequest request = new TransferDTO.CreateRequest(
                senderId, receiverId, BigDecimal.valueOf(100.00));

        String jsonRequest = objectMapper.writeValueAsString(request);

        CompletableFuture<ResultActions> call1 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/v1/api/transfer/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<ResultActions> call2 = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/v1/api/transfer/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        int status1 = call1.get().andReturn().getResponse().getStatus();
        int status2 = call2.get().andReturn().getResponse().getStatus();

        List<Integer> results = List.of(status1, status2);

        assertThat(results)
                .as("Deveria haver uma transferência com sucesso e uma falha por concorrência.")
                .containsExactlyInAnyOrder(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CONFLICT.value()
                );
    }
}
