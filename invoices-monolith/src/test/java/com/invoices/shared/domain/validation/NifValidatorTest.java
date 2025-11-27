package com.invoices.shared.domain.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NifValidatorTest {

    private NifValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new NifValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    void shouldReturnTrueForNullOrEmpty() {
        assertTrue(validator.isValid(null, context));
        assertTrue(validator.isValid("", context));
        assertTrue(validator.isValid("   ", context));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678Z", // Valid NIF
            "X1234567L", // Valid NIE (Foreigner)
            "B12345678" // Valid CIF structure (simplified check)
    })
    void shouldReturnTrueForValidTaxIds(String taxId) {
        assertTrue(validator.isValid(taxId, context));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678A", // Invalid letter for NIF
            "X1234567Z", // Invalid letter for NIE
            "INVALID", // Invalid format
            "123", // Too short
            "1234567890" // Too long
    })
    void shouldReturnFalseForInvalidTaxIds(String taxId) {
        assertFalse(validator.isValid(taxId, context));
    }
}
