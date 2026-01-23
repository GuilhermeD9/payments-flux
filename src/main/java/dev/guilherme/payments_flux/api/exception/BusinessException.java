package dev.guilherme.payments_flux.api.exception;

public class BusinessException extends ServiceException {
    public BusinessException(String message) {
        super(message);
    }
}
