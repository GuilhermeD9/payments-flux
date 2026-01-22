package dev.guilherme.payments_flux.core.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.constraintvalidators.hv.br.CNPJValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.br.CPFValidator;
import org.springframework.stereotype.Component;

@Component
public class CpfCnpjValidator implements ConstraintValidator<CpfCnpj, String> {

    private final CPFValidator cpfValidator = new CPFValidator();
    private final CNPJValidator cnpjValidator = new CNPJValidator();

    @Override
    public void initialize(CpfCnpj constraintAnnotation) {
        cpfValidator.initialize(null);
        cnpjValidator.initialize(null);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String cpfCnpj = value.replaceAll("[^0-9]", "");

        if (cpfCnpj.length() == 11) {
            return cpfValidator.isValid(cpfCnpj, context);
        } else if (cpfCnpj.length() > 11) {
            return cnpjValidator.isValid(cpfCnpj, context);
        }

        return false;
    }
}
