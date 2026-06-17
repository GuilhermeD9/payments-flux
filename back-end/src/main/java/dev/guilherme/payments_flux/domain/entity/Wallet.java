package dev.guilherme.payments_flux.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(collection = "wallets")
@Data @AllArgsConstructor @NoArgsConstructor
public class Wallet {

    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String cpfCnpj;

    @Indexed(unique = true)
    private String email;

    @JsonIgnore
    private String password;

    private BigDecimal balance;

    @Version
    private long version;
}
