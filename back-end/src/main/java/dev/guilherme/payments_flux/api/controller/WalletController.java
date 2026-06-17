package dev.guilherme.payments_flux.api.controller;

import dev.guilherme.payments_flux.api.dto.WalletDTO;
import dev.guilherme.payments_flux.domain.service.wallet.WalletService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Validated
@RequestMapping(value = "v1/api/wallet")
@AllArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletDTO.Response> create(@RequestBody @Valid WalletDTO.CreateRequest walletDTO) {
        WalletDTO.Response response = walletService.create(walletDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletDTO.Response> findById(@PathVariable String id) {
        WalletDTO.Response response = walletService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance/{id}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String id) {
        BigDecimal response = walletService.getBalance(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WalletDTO.Response>> findAll() {
        return ResponseEntity.ok(walletService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WalletDTO.Response> update(@PathVariable String id,
                                                     @RequestBody @Valid WalletDTO.UpdateRequest walletDTO) {
        WalletDTO.Response response = walletService.update(id, walletDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        walletService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/deposit/{id}")
    public ResponseEntity<WalletDTO.Response> deposit(@PathVariable String id,
                                                      @RequestBody @Valid WalletDTO.MoneyRequest depositDTO) {
        WalletDTO.Response response = walletService.deposit(id, depositDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw/{id}")
    public ResponseEntity<WalletDTO.Response> withdraw(@PathVariable String id,
                                                       @RequestBody @Valid WalletDTO.MoneyRequest withdrawDTO) {
        WalletDTO.Response response = walletService.withdraw(id, withdrawDTO);
        return ResponseEntity.ok(response);
    }
}
