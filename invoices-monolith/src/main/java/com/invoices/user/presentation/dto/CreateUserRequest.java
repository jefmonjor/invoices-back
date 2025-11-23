package com.invoices.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for creating a new user.
 * Includes validation constraints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    /**
     * Optional roles. If not provided, defaults to "ROLE_USER"
     */
    private Set<String> roles;

    // Company Registration Fields
    private String registrationType; // NEW_COMPANY, JOIN_COMPANY

    // For NEW_COMPANY
    private String companyName;
    private String taxId;
    private String companyAddress;
    private String companyEmail;
    private String companyPhone;

    // For JOIN_COMPANY
    private String invitationToken;
}
