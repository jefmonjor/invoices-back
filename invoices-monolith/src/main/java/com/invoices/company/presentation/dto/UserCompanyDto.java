package com.invoices.company.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a user within a company context.
 * Used for listing company users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCompanyDto {
    private Long userId;
    private String name;
    private String email;
    private String role; // ADMIN or USER
    private LocalDateTime joinedAt;
}
