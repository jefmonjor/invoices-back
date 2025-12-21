package com.invoices.company.presentation.controllers;

import com.invoices.company.application.services.CompanyManagementService;
import com.invoices.company.application.services.CompanyInvitationService;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.company.presentation.dto.CompanyDto;
import com.invoices.company.presentation.dto.CreateCompanyRequest;
import com.invoices.company.presentation.dto.CreateInvitationRequest;
import com.invoices.company.presentation.dto.InvitationResponse;
import com.invoices.company.presentation.dto.UpdateCompanyRequest;
import com.invoices.company.presentation.dto.UpdateRoleRequest;
import com.invoices.company.presentation.dto.CompanyMetricsDto;
import com.invoices.document.domain.services.StorageUrlResolver;
import com.invoices.security.JwtUtil;
import com.invoices.security.context.CompanyContext;
import com.invoices.user.domain.entities.User;
import com.invoices.user.presentation.dto.AuthResponse;
import com.invoices.user.presentation.mappers.UserDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Company Management", description = "Endpoints for managing companies and invitations")
public class CompanyController {

        private final CompanyManagementService companyManagementService;
        private final CompanyInvitationService companyInvitationService;
        private final JwtUtil jwtUtil;
        private final UserDtoMapper userDtoMapper;
        private final StorageUrlResolver storageUrlResolver;

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get my companies", description = "Retrieves the list of companies the authenticated user belongs to.", responses = {
                        @ApiResponse(responseCode = "200", description = "Companies retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<java.util.List<CompanyDto>> getUserCompanies(
                        @AuthenticationPrincipal UserDetails userDetails) {
                var companies = companyManagementService.getUserCompanies(userDetails.getUsername());
                return ResponseEntity.ok(companies);
        }

        /**
         * Alias endpoint for frontend compatibility.
         * GET /api/companies/my - same as GET /api/companies
         */
        @GetMapping("/my")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get my companies (alias)", description = "Alias for GET /api/companies. Retrieves the list of companies the authenticated user belongs to.", responses = {
                        @ApiResponse(responseCode = "200", description = "Companies retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        public ResponseEntity<java.util.List<CompanyDto>> getMyCompanies(
                        @AuthenticationPrincipal UserDetails userDetails) {
                return getUserCompanies(userDetails);
        }

        /**
         * Switch company endpoint for frontend compatibility.
         * POST /api/companies/switch/{companyId}
         */
        @PostMapping("/switch/{companyId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Switch company", description = "Switches the current company context and returns a new JWT token", responses = {
                        @ApiResponse(responseCode = "200", description = "Company switched successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid company ID"),
                        @ApiResponse(responseCode = "403", description = "User does not belong to the company")
        })
        public ResponseEntity<AuthResponse> switchCompany(
                        @PathVariable Long companyId,
                        @AuthenticationPrincipal UserDetails userDetails) {
                String email = userDetails.getUsername();
                log.info("POST /api/companies/switch/{} - User: {}", companyId, email);

                // Update user's current company in DB
                companyManagementService.switchCompany(email, companyId);

                // Fetch updated user to generate new token
                User updatedUser = companyManagementService.getUserByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found after switch"));

                // Generate new JWT token with updated companyId
                String token = jwtUtil.generateToken(updatedUser.getEmail(), updatedUser.getRoles(),
                                updatedUser.getCurrentCompanyId());

                AuthResponse response = AuthResponse.builder()
                                .token(token)
                                .expiresIn(jwtUtil.getExpirationTime())
                                .user(userDtoMapper.toDTO(updatedUser))
                                .build();

                log.info("Company switched successfully for user: {}", email);
                return ResponseEntity.ok(response);
        }

        @PostMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Create a new company", description = "Creates a company. New users can create their first company. Existing ADMINs can create additional companies and will automatically be assigned as ADMIN.", responses = {
                        @ApiResponse(responseCode = "200", description = "Company created successfully"),
                        @ApiResponse(responseCode = "401", description = "User not authenticated"),
                        @ApiResponse(responseCode = "409", description = "Company with this CIF already exists")
        })
        public ResponseEntity<CompanyDto> createCompany(
                        @Valid @RequestBody CreateCompanyRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                var userOpt = companyManagementService.getUserByEmail(userDetails.getUsername());
                if (userOpt.isEmpty()) {
                        throw new SecurityException("User not found");
                }
                Long userId = userOpt.get().getId();

                Company company = new Company(
                                null,
                                request.getBusinessName(),
                                request.getTaxId(),
                                request.getAddress(),
                                request.getCity(),
                                request.getPostalCode(),
                                request.getProvince(),
                                request.getPhone(),
                                request.getEmail(),
                                null // iban
                );

                Company created = companyManagementService.createAdditionalCompany(company, userId);
                return ResponseEntity.ok(CompanyDto.fromEntity(created, "ADMIN", false));
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Update company details", description = "**ADMIN only.** Updates company information. **Note:** CIF (Tax ID) cannot be modified after creation.", responses = {
                        @ApiResponse(responseCode = "200", description = "Company updated successfully"),
                        @ApiResponse(responseCode = "403", description = "User is not ADMIN in this company"),
                        @ApiResponse(responseCode = "404", description = "Company not found")
        })
        @Parameter(name = "id", description = "Company ID", required = true)
        public ResponseEntity<CompanyDto> updateCompany(
                        @PathVariable Long id,
                        @Valid @RequestBody UpdateCompanyRequest request) {

                Long currentCompanyId = CompanyContext.getCompanyId();
                if (!id.equals(currentCompanyId)) {
                        throw new SecurityException("Cannot update a company you are not currently managing");
                }

                Company updated = companyManagementService.updateCompany(id, request);
                return ResponseEntity.ok(CompanyDto.fromEntity(updated));
        }

        @GetMapping("/{id}/users")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "List all users in company", description = "**ADMIN only.** Returns all users associated with the company, including their roles.", responses = {
                        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "User is not ADMIN in this company"),
                        @ApiResponse(responseCode = "404", description = "Company not found")
        })
        @Parameter(name = "id", description = "Company ID", required = true)
        public ResponseEntity<com.invoices.company.presentation.dto.CompanyUsersResponse> getCompanyUsers(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetails userDetails) {

                var userOpt = companyManagementService.getUserByEmail(userDetails.getUsername());
                if (userOpt.isEmpty()) {
                        throw new SecurityException("User not found");
                }
                Long userId = userOpt.get().getId();

                var users = companyManagementService.getCompanyUsers(id, userId);
                return ResponseEntity.ok(
                                new com.invoices.company.presentation.dto.CompanyUsersResponse(users, users.size()));
        }

        @PostMapping("/{id}/invitations")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Generate invitation code", description = "**ADMIN only.** Generates a time-limited invitation code that allows new users to join the company. Default expiration: 168 hours (7 days).", responses = {
                        @ApiResponse(responseCode = "200", description = "Invitation code generated"),
                        @ApiResponse(responseCode = "403", description = "User is not ADMIN in this company")
        })
        @Parameter(name = "id", description = "Company ID", required = true)
        public ResponseEntity<InvitationResponse> generateInvitation(
                        @PathVariable Long id,
                        @Valid @RequestBody CreateInvitationRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                String code = companyInvitationService.createInvitation(id, userDetails.getUsername(),
                                request.getExpiresInHours());
                return ResponseEntity.ok(new InvitationResponse(code, request.getExpiresInHours()));
        }

        /**
         * Validate invitation code (public)
         */
        @GetMapping("/validate/{code}")
        @Operation(summary = "Validate invitation code", description = "**Public endpoint.** Validates an invitation code and returns the associated company details if valid. Used during user registration.", responses = {
                        @ApiResponse(responseCode = "200", description = "Code is valid, returns company details"),
                        @ApiResponse(responseCode = "404", description = "Invalid or expired code")
        })
        @Parameter(name = "code", description = "Invitation code", required = true, example = "ABC123XYZ")
        public ResponseEntity<CompanyDto> validateInvitation(@PathVariable String code) {
                Company company = companyInvitationService.validateInvitation(code);
                return ResponseEntity.ok(CompanyDto.fromEntity(company));
        }

        @DeleteMapping("/{id}/users/{userId}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Remove user from company", description = "**ADMIN only.** Removes a user from the company. Cannot remove the last ADMIN user.", responses = {
                        @ApiResponse(responseCode = "204", description = "User removed successfully"),
                        @ApiResponse(responseCode = "403", description = "User is not ADMIN"),
                        @ApiResponse(responseCode = "400", description = "Cannot remove last ADMIN"),
                        @ApiResponse(responseCode = "404", description = "User not found in company")
        })
        public ResponseEntity<Void> removeUserFromCompany(
                        @PathVariable Long id,
                        @PathVariable Long userId,
                        @AuthenticationPrincipal UserDetails userDetails) {

                var userOpt = companyManagementService.getUserByEmail(userDetails.getUsername());
                if (userOpt.isEmpty()) {
                        throw new SecurityException("User not found");
                }
                Long requestingUserId = userOpt.get().getId();

                companyManagementService.removeUserFromCompany(id, userId, requestingUserId);
                return ResponseEntity.noContent().build();
        }

        @PutMapping("/{id}/users/{userId}/role")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Update user role", description = "**ADMIN only.** Changes a user's role in the company (ADMIN or USER). Cannot demote the last ADMIN.", responses = {
                        @ApiResponse(responseCode = "200", description = "Role updated successfully"),
                        @ApiResponse(responseCode = "403", description = "User is not ADMIN"),
                        @ApiResponse(responseCode = "400", description = "Cannot demote last ADMIN or invalid role"),
                        @ApiResponse(responseCode = "404", description = "User not found in company")
        })
        public ResponseEntity<Void> updateUserRole(
                        @PathVariable Long id,
                        @PathVariable Long userId,
                        @Valid @RequestBody UpdateRoleRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {

                var userOpt = companyManagementService.getUserByEmail(userDetails.getUsername());
                if (userOpt.isEmpty()) {
                        throw new SecurityException("User not found");
                }
                Long requestingUserId = userOpt.get().getId();

                companyManagementService.updateUserRole(id, userId, request.getRole(), requestingUserId);
                return ResponseEntity.ok().build();
        }

        @GetMapping("/{id}/metrics")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Get company metrics", description = "**ADMIN only.** Returns metrics for the company including invoices, revenue, clients, and users.", responses = {
                        @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
                        @ApiResponse(responseCode = "403", description = "User is not ADMIN")
        })
        public ResponseEntity<CompanyMetricsDto> getCompanyMetrics(@PathVariable Long id) {
                CompanyMetricsDto metrics = companyManagementService.getCompanyMetrics(id);
                return ResponseEntity.ok(metrics);
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Delete company", description = "**ADMIN only.** Deletes a company. Cannot delete if there are existing invoices.", responses = {
                        @ApiResponse(responseCode = "204", description = "Company deleted successfully"),
                        @ApiResponse(responseCode = "403", description = "User is not ADMIN"),
                        @ApiResponse(responseCode = "400", description = "Company has existing invoices")
        })
        public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
                companyManagementService.deleteCompany(id);
                return ResponseEntity.noContent().build();
        }

        @PostMapping("/{id}/logo")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Upload company logo", description = "**ADMIN only.** Uploads a company logo. Accepts PNG only, max 500KB, recommended 180x60 pixels.", responses = {
                        @ApiResponse(responseCode = "200", description = "Logo uploaded successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid file format or size"),
                        @ApiResponse(responseCode = "403", description = "User is not ADMIN")
        })
        public ResponseEntity<CompanyDto> uploadLogo(
                        @PathVariable Long id,
                        @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

                // Validate file
                if (file.isEmpty()) {
                        throw new IllegalArgumentException("File is required");
                }

                String contentType = file.getContentType();
                if (contentType == null || !contentType.equals("image/png")) {
                        throw new IllegalArgumentException("Only PNG files are allowed");
                }

                // Max 500KB
                if (file.getSize() > 500 * 1024) {
                        throw new IllegalArgumentException("File size must be less than 500KB");
                }

                Long currentCompanyId = CompanyContext.getCompanyId();
                if (!id.equals(currentCompanyId)) {
                        throw new SecurityException("Cannot upload logo for a company you are not currently managing");
                }

                Company updated = companyManagementService.uploadLogo(id, file);
                return ResponseEntity.ok(CompanyDto.fromEntity(updated, storageUrlResolver));
        }

        @DeleteMapping("/{id}/logo")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Delete company logo", description = "**ADMIN only.** Removes the company logo.", responses = {
                        @ApiResponse(responseCode = "200", description = "Logo deleted successfully"),
                        @ApiResponse(responseCode = "403", description = "User is not ADMIN")
        })
        public ResponseEntity<CompanyDto> deleteLogo(@PathVariable Long id) {
                Long currentCompanyId = CompanyContext.getCompanyId();
                if (!id.equals(currentCompanyId)) {
                        throw new SecurityException("Cannot delete logo for a company you are not currently managing");
                }

                Company updated = companyManagementService.deleteLogo(id);
                return ResponseEntity.ok(CompanyDto.fromEntity(updated, storageUrlResolver));
        }
}
