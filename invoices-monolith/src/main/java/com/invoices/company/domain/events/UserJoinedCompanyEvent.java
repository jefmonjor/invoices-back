package com.invoices.company.domain.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when a new user joins a company via invitation.
 * Triggers email notifications to company ADMINs.
 */
@Getter
public class UserJoinedCompanyEvent extends ApplicationEvent {

    private final Long userId;
    private final Long companyId;
    private final String userEmail;
    private final String userName;

    public UserJoinedCompanyEvent(Object source, Long userId, Long companyId,
            String userEmail, String userName) {
        super(source);
        this.userId = userId;
        this.companyId = companyId;
        this.userEmail = userEmail;
        this.userName = userName;
    }
}
