package com.invoices.company.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CreateInvitationRequest {
    @Min(1)
    @Max(168) // Max 1 week
    private int expiresInHours = 24;
}
