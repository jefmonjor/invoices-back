package com.invoices.company.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InvitationResponse {
    private String code;
    private int expiresInHours;
}
