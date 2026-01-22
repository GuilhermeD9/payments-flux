package dev.guilherme.payments_flux.api.mapper;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.domain.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "receiver", ignore = true)
    Transfer toEntity(TransferDTO.CreateRequest request);

    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "receiverId", source = "receiver.id")
    TransferDTO.Response toResponse(Transfer transfer);
}
