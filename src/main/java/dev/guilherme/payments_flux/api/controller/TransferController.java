package dev.guilherme.payments_flux.api.controller;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.domain.service.transfer.TransferService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequestMapping(value = "v1/api/transfer")
@AllArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/create")
    public ResponseEntity<TransferDTO.Response> create(@RequestBody @Valid TransferDTO.CreateRequest transferDTO) {
        TransferDTO.Response response = transferService.create(transferDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<TransferDTO.Response> findById(@PathVariable UUID id) {
        TransferDTO.Response response = transferService.findById(id);
        return ResponseEntity.ok(response);
    }
}
