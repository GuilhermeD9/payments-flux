package dev.guilherme.payments_flux.api.exception;

import java.util.UUID;

public class ResourceNotFoundException extends ServiceException {

  public ResourceNotFoundException(String resource, Long id) {
    super(String.format("%s with id %d not found", resource, id));
  }

  public ResourceNotFoundException(String resource, UUID id) {
    super(String.format("%s with id %s not found", resource, id));
  }
}
