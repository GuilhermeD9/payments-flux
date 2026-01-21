package dev.guilherme.payments_flux.api.mapper;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.domain.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Transfer toEntity(TransferDTO.CreateRequest request);
    
    TransferDTO.Response toResponse(Transfer transfer);
}
