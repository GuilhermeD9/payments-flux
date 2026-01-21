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
    public ResponseEntity<WalletDTO.Response> findById(@RequestParam Long id) {
        WalletDTO.Response response = walletService.findById(id);
        return ResponseEntity.ok(response);
    }

}
