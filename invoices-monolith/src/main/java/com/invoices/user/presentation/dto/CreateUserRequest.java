package com.invoices.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import com.invoices.shared.domain.validation.ValidNif;
import com.invoices.shared.domain.utils.InputSanitizer;

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

    public void setFirstName(String firstName) {
        this.firstName = InputSanitizer.sanitize(firstName);
    }

    public void setLastName(String lastName) {
        this.lastName = InputSanitizer.sanitize(lastName);
    }

    /**
     * Optional roles. If not provided, defaults to "ROLE_USER"
     */
    private Set<String> roles;

    // Company Registration Fields
    private String registrationType; // NEW_COMPANY, JOIN_COMPANY

    // For NEW_COMPANY
    private String companyName;

    @ValidNif
    private String taxId;

    private String companyAddress;
    private String companyEmail;
    private String companyPhone;

    public void setCompanyName(String companyName) {
        this.companyName = InputSanitizer.sanitize(companyName);
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = InputSanitizer.sanitize(companyAddress);
    }

    // For JOIN_COMPANY
    private String invitationToken;
    private String invitationCode;
}
