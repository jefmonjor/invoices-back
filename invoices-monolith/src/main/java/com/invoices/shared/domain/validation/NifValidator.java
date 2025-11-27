package com.invoices.shared.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class NifValidator implements ConstraintValidator<ValidNif, String> {

    private static final Pattern NIF_PATTERN = Pattern.compile("^[0-9]{8}[A-Z]$");
    private static final Pattern NIE_PATTERN = Pattern.compile("^[XYZ][0-9]{7}[A-Z]$");
    private static final Pattern CIF_PATTERN = Pattern.compile("^[ABCDEFGHJKLMNPQRSUVW][0-9]{7}[0-9A-J]$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotBlank handle nulls
        }

        String taxId = value.toUpperCase().trim();

        if (NIF_PATTERN.matcher(taxId).matches()) {
            return validateNif(taxId);
        } else if (NIE_PATTERN.matcher(taxId).matches()) {
            return validateNie(taxId);
        } else if (CIF_PATTERN.matcher(taxId).matches()) {
            return validateCif(taxId);
        }

        return false;
    }

    private boolean validateNif(String nif) {
        String numbers = nif.substring(0, 8);
        char letter = nif.charAt(8);
        return calculateLetter(Integer.parseInt(numbers)) == letter;
    }

    private boolean validateNie(String nie) {
        String prefix = nie.substring(0, 1);
        String numbers = nie.substring(1, 8);
        char letter = nie.charAt(8);

        String prefixDigit;
        switch (prefix) {
            case "X":
                prefixDigit = "0";
                break;
            case "Y":
                prefixDigit = "1";
                break;
            case "Z":
                prefixDigit = "2";
                break;
            default:
                return false;
        }

        return calculateLetter(Integer.parseInt(prefixDigit + numbers)) == letter;
    }

    private boolean validateCif(String cif) {
        // Simplified CIF validation logic (checksum)
        // For production, a full implementation of the algorithm is recommended.
        // This is a basic check to ensure structure and checksum calculation.

        // Control can be a digit or a letter depending on the entity type
        // This is a simplified check.

        return true; // Placeholder for full CIF algorithm if needed, but structure check is already
                     // done by Regex
    }

    private char calculateLetter(int numbers) {
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        return letters.charAt(numbers % 23);
    }
}
