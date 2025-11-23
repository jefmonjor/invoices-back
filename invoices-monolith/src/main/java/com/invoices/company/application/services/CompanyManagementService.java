package com.invoices.company.application.services;

import com.invoices.company.infrastructure.persistence.entities.UserCompany;
import com.invoices.company.infrastructure.persistence.entities.UserCompanyId;
import com.invoices.company.infrastructure.persistence.repositories.UserCompanyRepository;
import com.invoices.invoice.domain.entities.Company;
import com.invoices.invoice.domain.ports.CompanyRepository;
import com.invoices.user.domain.entities.User;
import com.invoices.user.domain.ports.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyManagementService {

    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final UserRepository userRepository;

    public CompanyManagementService(CompanyRepository companyRepository,
            UserCompanyRepository userCompanyRepository,
            UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Company createCompany(Company company, Long userId) {
        // Save company
        Company savedCompany = companyRepository.save(company);

        // Link user to company as ADMIN
        UserCompanyId id = new UserCompanyId(userId, savedCompany.getId());
        UserCompany userCompany = new UserCompany(id, "ADMIN");
        userCompanyRepository.save(userCompany);

        // Update user's current company if not set
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getCurrentCompanyId() == null) {
                userRepository.save(user.withCurrentCompany(savedCompany.getId()));
            }
        }

        return savedCompany;
    }

    @Transactional
    public void addUserToCompany(Long userId, Long companyId, String role) {
        UserCompanyId id = new UserCompanyId(userId, companyId);
        UserCompany userCompany = new UserCompany(id, role);
        userCompanyRepository.save(userCompany);
    }

    public List<UserCompany> getUserCompanies(Long userId) {
        return userCompanyRepository.findByIdUserId(userId);
    }
}
