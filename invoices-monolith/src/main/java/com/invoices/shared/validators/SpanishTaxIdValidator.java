package com.invoices.shared.validators;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Validator for Spanish tax identification numbers (DNI, NIE, CIF)
 * 
 * - DNI: Documento Nacional de Identidad (8 digits + letter)
 * - NIE: Número de Identidad de Extranjero (X/Y/Z + 7 digits + letter)
 * - CIF: Código de Identificación Fiscal (letter + 7 digits + control)
 */
@Component
public class SpanishTaxIdValidator {

    private static final String DNI_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final Map<Character, Character> NIE_PREFIX_MAP = new HashMap<>();
    private static final Set<Character> LETTER_CONTROL_CIF = Set.of('K', 'P', 'Q', 'S');
    private static final String CIF_CONTROL_LETTERS = "JABCDEFGHI";

    static {
        NIE_PREFIX_MAP.put('X', '0');
        NIE_PREFIX_MAP.put('Y', '1');
        NIE_PREFIX_MAP.put('Z', '2');
    }

    /**
     * Validates a Spanish DNI
     */
    public boolean isValidDNI(String dni) {
        if (dni == null || dni.isEmpty()) {
            return false;
        }

        String clean = dni.toUpperCase().replaceAll("[^0-9A-Z]", "");

        if (!clean.matches("^\\d{8}[A-Z]$")) {
            return false;
        }

        int number = Integer.parseInt(clean.substring(0, 8));
        char letter = clean.charAt(8);
        char expectedLetter = DNI_LETTERS.charAt(number % 23);

        return letter == expectedLetter;
    }

    /**
     * Validates a Spanish NIE
     */
    public boolean isValidNIE(String nie) {
        if (nie == null || nie.isEmpty()) {
            return false;
        }

        String clean = nie.toUpperCase().replaceAll("[^0-9A-Z]", "");

        if (!clean.matches("^[XYZ]\\d{7}[A-Z]$")) {
            return false;
        }

        char prefix = clean.charAt(0);
        String nieAsNumber = NIE_PREFIX_MAP.get(prefix) + clean.substring(1, 8);
        int number = Integer.parseInt(nieAsNumber);
        char letter = clean.charAt(8);
        char expectedLetter = DNI_LETTERS.charAt(number % 23);

        return letter == expectedLetter;
    }

    /**
     * Validates a Spanish CIF
     */
    public boolean isValidCIF(String cif) {
        if (cif == null || cif.isEmpty()) {
            return false;
        }

        String clean = cif.toUpperCase().replaceAll("[^0-9A-Z]", "");

        if (!clean.matches("^[ABCDEFGHJNPQRSUVW]\\d{7}[0-9A-J]$")) {
            return false;
        }

        char organizationLetter = clean.charAt(0);
        String numbers = clean.substring(1, 8);
        char control = clean.charAt(8);

        // Calculate control digit
        int sum = 0;
        for (int i = 0; i < numbers.length(); i++) {
            int digit = Character.getNumericValue(numbers.charAt(i));
            if (i % 2 == 0) {
                // Even positions - double and sum digits
                int doubled = digit * 2;
                sum += (doubled / 10) + (doubled % 10);
            } else {
                // Odd positions
                sum += digit;
            }
        }

        int unitDigit = sum % 10;
        int controlDigit = (unitDigit == 0) ? 0 : 10 - unitDigit;

        // Organizations starting with K, P, Q, S use letter control
        boolean useLetterControl = LETTER_CONTROL_CIF.contains(organizationLetter);

        if (useLetterControl) {
            char controlLetter = CIF_CONTROL_LETTERS.charAt(controlDigit);
            return control == controlLetter;
        } else {
            return control == Character.forDigit(controlDigit, 10) ||
                    control == CIF_CONTROL_LETTERS.charAt(controlDigit);
        }
    }

    /**
     * Validates any Spanish tax ID (DNI, NIE, or CIF)
     */
    public ValidationResult validate(String taxId) {
        if (taxId == null || taxId.trim().isEmpty()) {
            return new ValidationResult(false, TaxIdType.UNKNOWN, "Tax ID is required");
        }

        String clean = taxId.toUpperCase().replaceAll("[^0-9A-Z]", "");

        // Check DNI
        if (clean.matches("^\\d{8}[A-Z]$")) {
            boolean valid = isValidDNI(clean);
            return new ValidationResult(
                    valid,
                    TaxIdType.DNI,
                    valid ? "Valid DNI" : "Invalid DNI - Incorrect control letter");
        }

        // Check NIE
        if (clean.matches("^[XYZ]\\d{7}[A-Z]$")) {
            boolean valid = isValidNIE(clean);
            return new ValidationResult(
                    valid,
                    TaxIdType.NIE,
                    valid ? "Valid NIE" : "Invalid NIE - Incorrect control letter");
        }

        // Check CIF
        if (clean.matches("^[ABCDEFGHJNPQRSUVW]\\d{7}[0-9A-J]$")) {
            boolean valid = isValidCIF(clean);
            return new ValidationResult(
                    valid,
                    TaxIdType.CIF,
                    valid ? "Valid CIF" : "Invalid CIF - Incorrect control digit/letter");
        }

        return new ValidationResult(
                false,
                TaxIdType.UNKNOWN,
                "Unrecognized tax ID format (must be DNI, NIE, or CIF)");
    }

    public enum TaxIdType {
        DNI, NIE, CIF, UNKNOWN
    }

    public static class ValidationResult {
        private final boolean valid;
        private final TaxIdType type;
        private final String message;

        public ValidationResult(boolean valid, TaxIdType type, String message) {
            this.valid = valid;
            this.type = type;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public TaxIdType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }
}
