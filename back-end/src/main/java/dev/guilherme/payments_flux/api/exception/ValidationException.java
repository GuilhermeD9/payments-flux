package dev.guilherme.payments_flux.api.exception;

public class ValidationException extends ServiceException {
    public ValidationException(String message) {
        super(message);
    }
}
