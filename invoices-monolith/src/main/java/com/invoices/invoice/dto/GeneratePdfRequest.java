package com.invoices.invoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePdfRequest {

    @NotNull(message = "El ID de la factura es obligatorio")
    private Long invoiceId;
}
