package dev.guilherme.payments_flux.core.validator;

import br.com.caelum.stella.validation.InvalidStateException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.CNPJValidator;

@Component
public class CpfCnpjValidator implements ConstraintValidator<CPFCNPJ, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String cpfCnpj = value.replaceAll("[^0-9]", "");

        try {
            if (cpfCnpj.length() == 11) {
                new CPFValidator().assertValid(cpfCnpj);
                return true;
            } else if (cpfCnpj.length() > 11) {
                new CNPJValidator().assertValid(cpfCnpj);
                return true;
            }
        } catch (InvalidStateException e) {
            return false;
        }

        return false;
    }
}
