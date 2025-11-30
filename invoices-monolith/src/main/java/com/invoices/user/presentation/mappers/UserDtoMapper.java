package com.invoices.user.presentation.mappers;

import com.invoices.user.domain.entities.PlatformRole;
import com.invoices.user.domain.entities.User;
import com.invoices.user.presentation.dto.UserDTO;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper to convert between domain User and presentation UserDTO.
 * This keeps the presentation layer independent from domain details.
 */
@Component
public class UserDtoMapper {

    /**
     * Convert domain User to UserDTO
     *
     * @param domainUser the domain user
     * @return UserDTO for presentation
     */
    public UserDTO toDTO(User domainUser) {
        if (domainUser == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(domainUser.getId());
        dto.setEmail(domainUser.getEmail());
        dto.setFirstName(domainUser.getFirstName());
        dto.setLastName(domainUser.getLastName());
        dto.setRoles(domainUser.getRoles().stream().collect(Collectors.toSet()));
        if (domainUser.getPlatformRole() == PlatformRole.PLATFORM_ADMIN) {
            dto.getRoles().add("ROLE_PLATFORM_ADMIN");
        }
        dto.setEnabled(domainUser.isEnabled());
        dto.setCurrentCompanyId(domainUser.getCurrentCompanyId());
        dto.setCreatedAt(domainUser.getCreatedAt());
        dto.setUpdatedAt(domainUser.getUpdatedAt());

        return dto;
    }

    /**
     * Convert UserDTO to domain User (for updates only - doesn't include password)
     * Note: This is typically not used for creating new users (use Use Cases
     * directly)
     *
     * @param dto the UserDTO
     * @return partial domain user
     */
    public User toDomainEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }

        // Note: Password is not included - must be handled separately
        return new User(
                dto.getId(),
                dto.getEmail(),
                null, // Password must be set separately
                dto.getFirstName(),
                dto.getLastName(),
                dto.getRoles(),
                dto.getEnabled(),
                true, // accountNonExpired
                true, // accountNonLocked
                true, // credentialsNonExpired
                dto.getCreatedAt(),
                dto.getUpdatedAt(),
                null, // lastLogin
                dto.getCurrentCompanyId(),
                PlatformRole.REGULAR_USER); // Default to REGULAR_USER for DTO mapping
    }
}
