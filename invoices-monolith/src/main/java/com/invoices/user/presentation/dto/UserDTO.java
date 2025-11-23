package com.invoices.user.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for User entity responses.
 * Does not include password for security.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    private String email;

    private String firstName;

    private String lastName;

    private Set<String> roles;

    private Boolean enabled;

    private Long currentCompanyId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
