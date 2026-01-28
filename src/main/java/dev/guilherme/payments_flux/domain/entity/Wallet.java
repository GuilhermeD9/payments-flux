package dev.guilherme.payments_flux.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_wallet")
@Data @AllArgsConstructor @NoArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    @Column(unique = true)
    private String cpfCnpj;

    @Column(unique = true)
    private String email;

    @JsonIgnore
    private String password;

    private BigDecimal balance;

    @Version
    private long version;

    @PrePersist
    @PreUpdate
    public void cleanDocs() {
        if (this.cpfCnpj != null) {
            this.cpfCnpj = this.cpfCnpj.replaceAll("\\D", "");
        }
    }
}
