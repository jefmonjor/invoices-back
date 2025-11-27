package com.invoices.company.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing the list of users in a company.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompanyUsersResponse {
    private List<UserCompanyDto> users;
    private int totalCount;
}
