package com.invoices.shared.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigInteger;

public class IbanValidator implements ConstraintValidator<ValidIban, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotBlank handle nulls
        }

        String iban = value.trim().toUpperCase().replaceAll("\\s+", "");

        if (iban.length() < 15 || iban.length() > 34) {
            return false;
        }

        // Move first 4 chars to end
        String reordered = iban.substring(4) + iban.substring(0, 4);

        // Convert letters to numbers (A=10, B=11, ..., Z=35)
        StringBuilder numericIban = new StringBuilder();
        for (char c : reordered.toCharArray()) {
            if (Character.isDigit(c)) {
                numericIban.append(c);
            } else if (Character.isLetter(c)) {
                numericIban.append(c - 'A' + 10);
            } else {
                return false; // Invalid character
            }
        }

        // Modulo 97 check
        BigInteger number = new BigInteger(numericIban.toString());
        return number.mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
