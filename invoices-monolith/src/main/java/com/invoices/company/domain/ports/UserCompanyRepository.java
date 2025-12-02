package com.invoices.company.domain.ports;

import com.invoices.company.domain.entities.UserCompany;
import com.invoices.company.domain.entities.UserCompanyId;

import java.util.List;
import java.util.Optional;

/**
 * Port for accessing UserCompany data (user-company relationships).
 * This is a domain interface that will be implemented by infrastructure layer.
 * Keeps domain layer independent of database implementation.
 */
public interface UserCompanyRepository {

    /**
     * Find user-company relationship by composite ID.
     *
     * @param id the composite ID
     * @return Optional containing the relationship if found
     */
    Optional<UserCompany> findById(UserCompanyId id);

    /**
     * Find all user-company relationships for a user with company data fetched.
     *
     * @param userId the user ID
     * @return list of user-company relationships
     */
    List<UserCompany> findByIdUserIdWithCompanyFetch(Long userId);

    /**
     * Find all user-company relationships for a user.
     *
     * @param userId the user ID
     * @return list of user-company relationships
     */
    List<UserCompany> findByIdUserId(Long userId);

    /**
     * Find all user-company relationships for a company with user data fetched.
     *
     * @param companyId the company ID
     * @return list of user-company relationships
     */
    List<UserCompany> findByIdCompanyIdWithUserFetch(Long companyId);

    /**
     * Find all user-company relationships for a company.
     *
     * @param companyId the company ID
     * @return list of user-company relationships
     */
    List<UserCompany> findByIdCompanyId(Long companyId);

    /**
     * Count users with a specific role in a company.
     *
     * @param companyId the company ID
     * @param role      the role to count
     * @return number of users with that role
     */
    long countByIdCompanyIdAndRole(Long companyId, String role);

    /**
     * Count all users in a company.
     *
     * @param companyId the company ID
     * @return number of users in the company
     */
    long countByIdCompanyId(Long companyId);

    /**
     * Save a user-company relationship.
     *
     * @param userCompany the relationship to save
     * @return the saved relationship
     */
    UserCompany save(UserCompany userCompany);

    /**
     * Delete a user-company relationship.
     *
     * @param userCompany the relationship to delete
     */
    void delete(UserCompany userCompany);

    /**
     * Delete all user-company relationships for a company.
     *
     * @param companyId the company ID
     */
    void deleteByIdCompanyId(Long companyId);

    /**
     * Delete a list of user-company relationships.
     *
     * @param userCompanies the list of relationships to delete
     */
    void deleteAll(List<UserCompany> userCompanies);

    /**
     * Delete all user-company relationships for a user.
     *
     * @param userId the user ID
     */
    void deleteByIdUserId(Long userId);
}
