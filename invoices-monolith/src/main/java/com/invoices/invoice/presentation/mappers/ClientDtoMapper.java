package com.invoices.invoice.presentation.mappers;

import com.invoices.invoice.domain.entities.Client;
import com.invoices.invoice.dto.ClientDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper between domain Client and ClientDTO.
 * Keeps domain and presentation layers decoupled.
 */
@Component
public class ClientDtoMapper {

    public ClientDTO toDto(Client client) {
        if (client == null) {
            return null;
        }

        return ClientDTO.builder()
            .id(client.getId())
            .businessName(client.getBusinessName())
            .taxId(client.getTaxId())
            .address(client.getAddress())
            .city(client.getCity())
            .postalCode(client.getPostalCode())
            .province(client.getProvince())
            .phone(client.getPhone())
            .email(client.getEmail())
            .build();
    }
}
