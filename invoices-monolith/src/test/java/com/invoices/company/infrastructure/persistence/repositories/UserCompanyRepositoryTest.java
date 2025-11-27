package com.invoices.company.infrastructure.persistence.repositories;

import com.invoices.company.infrastructure.persistence.entities.UserCompany;
import com.invoices.company.infrastructure.persistence.entities.UserCompanyId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for UserCompanyRepository
 * Tests JPA queries for user-company associations
 */
@DataJpaTest
@DisplayName("UserCompanyRepository Tests")
class UserCompanyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserCompanyRepository userCompanyRepository;

    @Test
    @DisplayName("findByIdUserId returns all companies for user")
    void findByIdUserId_UserHasCompanies_ReturnsCompanies() {
        // Given
        Long userId = 1L;
        Long companyId1 = 100L;
        Long companyId2 = 200L;

        UserCompany uc1 = createUserCompany(userId, companyId1, "ADMIN");
        UserCompany uc2 = createUserCompany(userId, companyId2, "USER");

        entityManager.persist(uc1);
        entityManager.persist(uc2);
        entityManager.flush();

        // When
        List<UserCompany> result = userCompanyRepository.findByIdUserId(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(uc -> uc.getId().getCompanyId())
                .containsExactlyInAnyOrder(companyId1, companyId2);
    }

    @Test
    @DisplayName("findByIdUserId returns empty list when user has no companies")
    void findByIdUserId_NoCompanies_ReturnsEmptyList() {
        // When
        List<UserCompany> result = userCompanyRepository.findByIdUserId(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdCompanyId returns all users in company")
    void findByIdCompanyId_CompanyHasUsers_ReturnsUsers() {
        // Given
        Long companyId = 100L;
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        UserCompany uc1 = createUserCompany(userId1, companyId, "ADMIN");
        UserCompany uc2 = createUserCompany(userId2, companyId, "USER");
        UserCompany uc3 = createUserCompany(userId3, companyId, "USER");

        entityManager.persist(uc1);
        entityManager.persist(uc2);
        entityManager.persist(uc3);
        entityManager.flush();

        // When
        List<UserCompany> result = userCompanyRepository.findByIdCompanyId(companyId);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(uc -> uc.getId().getUserId())
                .containsExactlyInAnyOrder(userId1, userId2, userId3);
    }

    @Test
    @DisplayName("findByIdCompanyId returns empty list when company has no users")
    void findByIdCompanyId_NoUsers_ReturnsEmptyList() {
        // When
        List<UserCompany> result = userCompanyRepository.findByIdCompanyId(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findById returns UserCompany when association exists")
    void findById_AssociationExists_ReturnsUserCompany() {
        // Given
        Long userId = 1L;
        Long companyId = 100L;
        UserCompany uc = createUserCompany(userId, companyId, "ADMIN");

        entityManager.persist(uc);
        entityManager.flush();

        UserCompanyId id = new UserCompanyId(userId, companyId);

        // When
        Optional<UserCompany> result = userCompanyRepository.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("findById returns empty when association does not exist")
    void findById_AssociationNotExists_ReturnsEmpty() {
        // Given
        UserCompanyId id = new UserCompanyId(999L, 999L);

        // When
        Optional<UserCompany> result = userCompanyRepository.findById(id);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("save persists new UserCompany")
    void save_NewUserCompany_Persists() {
        // Given
        UserCompany uc = createUserCompany(1L, 100L, "USER");

        // When
        UserCompany saved = userCompanyRepository.save(uc);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo("USER");

        UserCompany found = entityManager.find(UserCompany.class, saved.getId());
        assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("delete removes UserCompany association")
    void delete_ExistingUserCompany_Removes() {
        // Given
        UserCompany uc = createUserCompany(1L, 100L, "USER");
        entityManager.persist(uc);
        entityManager.flush();

        UserCompanyId id = uc.getId();

        // When
        userCompanyRepository.delete(uc);
        entityManager.flush();

        // Then
        UserCompany found = entityManager.find(UserCompany.class, id);
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("Role filter works correctly")
    void findByIdCompanyId_FilterByRole_ReturnsOnlyMatchingRole() {
        // Given
        Long companyId = 100L;

        UserCompany admin1 = createUserCompany(1L, companyId, "ADMIN");
        UserCompany admin2 = createUserCompany(2L, companyId, "ADMIN");
        UserCompany user1 = createUserCompany(3L, companyId, "USER");

        entityManager.persist(admin1);
        entityManager.persist(admin2);
        entityManager.persist(user1);
        entityManager.flush();

        // When
        List<UserCompany> allUsers = userCompanyRepository.findByIdCompanyId(companyId);
        long adminCount = allUsers.stream()
                .filter(uc -> "ADMIN".equals(uc.getRole()))
                .count();

        // Then
        assertThat(allUsers).hasSize(3);
        assertThat(adminCount).isEqualTo(2);
    }

    // Helper method to create UserCompany instances
    private UserCompany createUserCompany(Long userId, Long companyId, String role) {
        UserCompanyId id = new UserCompanyId(userId, companyId);
        UserCompany uc = new UserCompany();
        uc.setId(id);
        uc.setRole(role);
        return uc;
    }
}
