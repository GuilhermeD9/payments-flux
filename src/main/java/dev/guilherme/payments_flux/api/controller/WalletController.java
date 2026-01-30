package dev.guilherme.payments_flux.api.controller;

import dev.guilherme.payments_flux.api.dto.WalletDTO;
import dev.guilherme.payments_flux.domain.service.wallet.WalletService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping(value = "v1/api/wallet")
@AllArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/create")
    public ResponseEntity<WalletDTO.Response> create(@RequestBody @Valid WalletDTO.CreateRequest walletDTO) {
        WalletDTO.Response response = walletService.create(walletDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<WalletDTO.Response> findById(@PathVariable Long id) {
        WalletDTO.Response response = walletService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<WalletDTO.Response> update(@PathVariable Long id,
                                                     @RequestBody @Valid WalletDTO.UpdateRequest walletDTO) {
        WalletDTO.Response response = walletService.update(id, walletDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        walletService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/deposit/{id}")
    public ResponseEntity<WalletDTO.Response> deposit(@PathVariable Long id,
                                                      @RequestBody @Valid WalletDTO.MoneyRequest depositDTO) {
        WalletDTO.Response response = walletService.deposit(id, depositDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw/{id}")
    public ResponseEntity<WalletDTO.Response> withdraw(@PathVariable Long id,
                                                       @RequestBody @Valid WalletDTO.MoneyRequest withdrawDTO) {
        WalletDTO.Response response = walletService.withdraw(id, withdrawDTO);
        return ResponseEntity.ok(response);
    }
}
