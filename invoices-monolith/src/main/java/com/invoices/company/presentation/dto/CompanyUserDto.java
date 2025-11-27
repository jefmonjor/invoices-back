package com.invoices.company.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyUserDto {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role; // ADMIN or USER
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
