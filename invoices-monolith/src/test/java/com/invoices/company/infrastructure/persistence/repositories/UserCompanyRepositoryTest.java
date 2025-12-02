package com.invoices.company.infrastructure.persistence.repositories;

import com.invoices.company.infrastructure.persistence.entities.UserCompany;
import com.invoices.company.infrastructure.persistence.entities.UserCompanyId;
import com.invoices.invoice.infrastructure.persistence.entities.CompanyJpaEntity;
import com.invoices.user.infrastructure.persistence.entities.UserJpaEntity;
import com.invoices.shared.domain.ports.EncryptionService;
import com.invoices.shared.infrastructure.security.encryption.EncryptedStringConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;

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

    @MockBean
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() throws Exception {
        // Mock encryption to return the input string (identity)
        Mockito.when(encryptionService.encrypt(Mockito.anyString())).thenAnswer(i -> i.getArguments()[0]);
        Mockito.when(encryptionService.decrypt(Mockito.anyString())).thenAnswer(i -> i.getArguments()[0]);

        // Manually inject into the static field of the converter
        // We create a new instance just to call the setter, which sets the static field
        new EncryptedStringConverter().setEncryptionService(encryptionService);
    }

    @Test
    @DisplayName("findByIdUserId returns all companies for user")
    void findByIdUserId_UserHasCompanies_ReturnsCompanies() {
        // Given
        UserJpaEntity user = createUser("user1@test.com");
        CompanyJpaEntity company1 = createCompany("B11111111");
        CompanyJpaEntity company2 = createCompany("B22222222");

        UserCompany uc1 = createUserCompany(user, company1, "ADMIN");
        UserCompany uc2 = createUserCompany(user, company2, "USER");

        entityManager.persist(uc1);
        entityManager.persist(uc2);
        entityManager.flush();

        // When
        List<UserCompany> result = userCompanyRepository.findByIdUserId(user.getId());

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(uc -> uc.getId().getCompanyId())
                .containsExactlyInAnyOrder(company1.getId(), company2.getId());
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
        CompanyJpaEntity company = createCompany("B33333333");
        UserJpaEntity user1 = createUser("user2@test.com");
        UserJpaEntity user2 = createUser("user3@test.com");
        UserJpaEntity user3 = createUser("user4@test.com");

        UserCompany uc1 = createUserCompany(user1, company, "ADMIN");
        UserCompany uc2 = createUserCompany(user2, company, "USER");
        UserCompany uc3 = createUserCompany(user3, company, "USER");

        entityManager.persist(uc1);
        entityManager.persist(uc2);
        entityManager.persist(uc3);
        entityManager.flush();

        // When
        List<UserCompany> result = userCompanyRepository.findByIdCompanyId(company.getId());

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(uc -> uc.getId().getUserId())
                .containsExactlyInAnyOrder(user1.getId(), user2.getId(), user3.getId());
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
        UserJpaEntity user = createUser("user5@test.com");
        CompanyJpaEntity company = createCompany("B44444444");
        UserCompany uc = createUserCompany(user, company, "ADMIN");

        entityManager.persist(uc);
        entityManager.flush();

        UserCompanyId id = new UserCompanyId(user.getId(), company.getId());

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
        UserJpaEntity user = createUser("user6@test.com");
        CompanyJpaEntity company = createCompany("B55555555");
        UserCompany uc = createUserCompany(user, company, "USER");

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
        UserJpaEntity user = createUser("user7@test.com");
        CompanyJpaEntity company = createCompany("B66666666");
        UserCompany uc = createUserCompany(user, company, "USER");
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
        CompanyJpaEntity company = createCompany("B77777777");
        UserJpaEntity admin1 = createUser("admin1@test.com");
        UserJpaEntity admin2 = createUser("admin2@test.com");
        UserJpaEntity user1 = createUser("user8@test.com");

        UserCompany ucAdmin1 = createUserCompany(admin1, company, "ADMIN");
        UserCompany ucAdmin2 = createUserCompany(admin2, company, "ADMIN");
        UserCompany ucUser1 = createUserCompany(user1, company, "USER");

        entityManager.persist(ucAdmin1);
        entityManager.persist(ucAdmin2);
        entityManager.persist(ucUser1);
        entityManager.flush();

        // When
        List<UserCompany> allUsers = userCompanyRepository.findByIdCompanyId(company.getId());
        long adminCount = allUsers.stream()
                .filter(uc -> "ADMIN".equals(uc.getRole()))
                .count();

        // Then
        assertThat(allUsers).hasSize(3);
        assertThat(adminCount).isEqualTo(2);
    }

    // Helper methods

    private UserJpaEntity createUser(String email) {
        UserJpaEntity user = new UserJpaEntity();
        user.setEmail(email);
        user.setPassword("password");
        user.setFirstName("Test");
        user.setLastName("User");
        return entityManager.persist(user);
    }

    private CompanyJpaEntity createCompany(String taxId) {
        CompanyJpaEntity company = new CompanyJpaEntity();
        company.setBusinessName("Test Company " + taxId);
        company.setTaxId(taxId);
        company.setAddress("Test Address");
        return entityManager.persist(company);
    }

    private UserCompany createUserCompany(UserJpaEntity user, CompanyJpaEntity company, String role) {
        UserCompanyId id = new UserCompanyId(user.getId(), company.getId());
        UserCompany uc = new UserCompany();
        uc.setId(id);
        uc.setRole(role);
        uc.setUser(user);
        uc.setCompany(company);
        return uc;
    }
}
