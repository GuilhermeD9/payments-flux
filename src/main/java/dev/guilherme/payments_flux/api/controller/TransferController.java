package dev.guilherme.payments_flux.api.controller;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.domain.service.transfer.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/findAll")
    public ResponseEntity<Page<TransferDTO.Response>> findAll(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(transferService.findAll(pageable));
    }

    @GetMapping("/find/sender/{id}")
    public ResponseEntity<List<TransferDTO.Response>> findBySender(@PathVariable @Min(0) Long id) {
        return ResponseEntity.ok(transferService.findBySender(id));
    }

    @GetMapping("/find/receiver/{id}")
    public ResponseEntity<List<TransferDTO.Response>> findByReceiver(@PathVariable @Min(0) Long id) {
        return ResponseEntity.ok(transferService.findByReceiver(id));
    }
}
