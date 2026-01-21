package dev.guilherme.payments_flux.api.mapper;

import dev.guilherme.payments_flux.api.dto.WalletDTO;
import dev.guilherme.payments_flux.domain.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    WalletDTO.Response toResponse(Wallet wallet);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "balance", ignore = true)
    Wallet toEntity(WalletDTO.CreateRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "cpfCnpj", ignore = true)
    @Mapping(target = "balance", ignore = true)
    void updateEntity(WalletDTO.UpdateRequest request, @MappingTarget Wallet wallet);
}
