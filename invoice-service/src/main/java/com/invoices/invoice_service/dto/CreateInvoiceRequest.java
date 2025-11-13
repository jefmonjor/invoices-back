package com.invoices.invoice_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clientId;

    @NotBlank(message = "El email del cliente es obligatorio")
    @Email(message = "El email del cliente debe ser válido")
    private String clientEmail;

    @NotNull(message = "La fecha de la factura es obligatoria")
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @NotEmpty(message = "La factura debe tener al menos un item")
    @Valid
    private List<CreateInvoiceItemRequest> items;

    private String notes;
}
