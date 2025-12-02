package com.invoices.company.infrastructure.persistence.repositories;

import com.invoices.company.infrastructure.persistence.entities.UserCompany;
import com.invoices.company.infrastructure.persistence.entities.UserCompanyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserCompanyRepository extends JpaRepository<UserCompany, UserCompanyId> {

    @Query("SELECT uc FROM UserCompany uc JOIN FETCH uc.company WHERE uc.id.userId = :userId")
    List<UserCompany> findByIdUserIdWithCompanyFetch(@Param("userId") Long userId);

    @Query("SELECT uc FROM UserCompany uc JOIN FETCH uc.user WHERE uc.id.companyId = :companyId")
    List<UserCompany> findByIdCompanyIdWithUserFetch(@Param("companyId") Long companyId);

    List<UserCompany> findByIdUserId(Long userId);

    List<UserCompany> findByIdCompanyId(Long companyId);

    void deleteByIdCompanyId(Long companyId);

    void deleteByIdUserId(Long userId);

    long countByIdCompanyId(Long companyId);

    @Query("SELECT COUNT(uc) FROM UserCompany uc WHERE uc.id.companyId = :companyId AND uc.role = :role")
    long countByIdCompanyIdAndRole(@Param("companyId") Long companyId, @Param("role") String role);
}
