package com.invoices.invoice.domain.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Client domain entity.
 * Tests business logic and validations without framework dependencies.
 */
class ClientTest {

    @Test
    void shouldCreateClientWithValidData() {
        // Arrange & Act
        Client client = new Client(
                1L,
                "SERSFRITRUCKS, S.A.",
                "A50008588",
                "JIMÉNEZ DE LA ESPADA, 57, BAJO",
                "CARTAGENA",
                "30203",
                "MURCIA",
                "968123456",
                "info@sersfritrucks.com",
                1L);

        // Assert
        assertThat(client).isNotNull();
        assertThat(client.getId()).isEqualTo(1L);
        assertThat(client.getBusinessName()).isEqualTo("SERSFRITRUCKS, S.A.");
        assertThat(client.getTaxId()).isEqualTo("A50008588");
        assertThat(client.getEmail()).isEqualTo("info@sersfritrucks.com");
    }

    @Test
    void shouldThrowExceptionWhenBusinessNameIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new Client(
                1L,
                null,
                "A12345678",
                "Address",
                "City",
                "12345",
                "Province",
                "123456789",
                "email@test.com",
                1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Business name cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenBusinessNameIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> new Client(
                1L,
                "   ",
                "A12345678",
                "Address",
                "City",
                "12345",
                "Province",
                "123456789",
                "email@test.com",
                1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Business name cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenTaxIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> new Client(
                1L,
                "Client Name",
                null,
                "Address",
                "City",
                "12345",
                "Province",
                "123456789",
                "email@test.com",
                1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tax ID cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenTaxIdIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> new Client(
                1L,
                "Client Name",
                "  ",
                "Address",
                "City",
                "12345",
                "Province",
                "123456789",
                "email@test.com",
                1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tax ID cannot be null or empty");
    }

    @Test
    void shouldReturnFullAddressWithAllFields() {
        // Arrange
        Client client = new Client(
                1L,
                "SERSFRITRUCKS, S.A.",
                "A50008588",
                "JIMÉNEZ DE LA ESPADA, 57, BAJO",
                "CARTAGENA",
                "30203",
                "MURCIA",
                "968123456",
                "info@sersfritrucks.com",
                1L);

        // Act
        String fullAddress = client.getFullAddress();

        // Assert
        assertThat(fullAddress).contains("JIMÉNEZ DE LA ESPADA, 57, BAJO");
        assertThat(fullAddress).contains("30203");
        assertThat(fullAddress).contains("CARTAGENA");
        assertThat(fullAddress).contains("MURCIA");
    }
}
