package com.invoices.company.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|USER", message = "Role must be either ADMIN or USER")
    private String role;
}
