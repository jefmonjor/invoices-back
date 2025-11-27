package com.invoices.invoice.domain.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Company domain entity.
 * Tests business logic and validations without framework dependencies.
 */
class CompanyTest {

    @Test
    void shouldCreateCompanyWithValidData() {
        // Arrange & Act
        Company company = new Company(
                1L,
                "TRANSOLIDO S.L.",
                "B91923755",
                "Castillo Lastrucci, 3, 3D",
                "DOS HERMANAS",
                "41701",
                "SEVILLA",
                "659889201",
                "contacto@transolido.es",
                "ES60 0182 4840 0022 0165 7539");

        // Assert
        assertThat(company).isNotNull();
        assertThat(company.getId()).isEqualTo(1L);
        assertThat(company.getBusinessName()).isEqualTo("TRANSOLIDO S.L.");
        assertThat(company.getTaxId()).isEqualTo("B91923755");
        assertThat(company.getIban()).isEqualTo("ES60 0182 4840 0022 0165 7539");
    }

    @Test
    void shouldThrowExceptionWhenBusinessNameIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new Company(
                1L,
                null,
                "B91923755",
                "Address",
                "City",
                "12345",
                "Province",
                "123456789",
                "email@test.com",
                "ES1234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Business name cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenBusinessNameIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> new Company(
                1L,
                "   ",
                "B91923755",
                "Address",
                "City",
                "12345",
                "Province",
                "123456789",
                "email@test.com",
                "ES1234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Business name cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenTaxIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new Company(
                1L,
                "Company Name",
                null,
                "Address",
                "City",
                "12345",
                "Province",
                "123456789",
                "email@test.com",
                "ES1234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tax ID cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenTaxIdIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> new Company(
                1L,
                "Company Name",
                "  ",
                "Address",
                "City",
                "12345",
                "Province",
                "123456789",
                "email@test.com",
                "ES1234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tax ID cannot be null or empty");
    }

    @Test
    void shouldReturnFullAddressWithAllFields() {
        // Arrange
        Company company = new Company(
                1L,
                "TRANSOLIDO S.L.",
                "B91923755",
                "Castillo Lastrucci, 3, 3D",
                "DOS HERMANAS",
                "41701",
                "SEVILLA",
                "659889201",
                "contacto@transolido.es",
                "ES60 0182 4840 0022 0165 7539");

        // Act
        String fullAddress = company.getFullAddress();

        // Assert
        assertThat(fullAddress).contains("Castillo Lastrucci, 3, 3D");
        assertThat(fullAddress).contains("41701");
        assertThat(fullAddress).contains("DOS HERMANAS");
        assertThat(fullAddress).contains("SEVILLA");
    }

    @Test
    void shouldReturnFullAddressWithPartialFields() {
        // Arrange
        Company company = new Company(
                1L,
                "Company",
                "B12345678",
                "Street 123",
                null,
                "12345",
                null,
                null,
                null,
                null);

        // Act
        String fullAddress = company.getFullAddress();

        // Assert
        assertThat(fullAddress).contains("Street 123");
        assertThat(fullAddress).contains("12345");
    }

    @Test
    void shouldReturnFullAddressWithOnlyAddress() {
        // Arrange
        Company company = new Company(
                1L,
                "Company",
                "B12345678",
                "Street 123",
                null,
                null,
                null,
                null,
                null,
                null);

        // Act
        String fullAddress = company.getFullAddress();

        // Assert
        assertThat(fullAddress).isEqualTo("Street 123, España");
    }
}
