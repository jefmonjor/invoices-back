package com.invoices.onboarding.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "demo_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_type")
    private String templateType; // CLIENT, INVOICE

    @Column(name = "template_data")
    @JdbcTypeCode(SqlTypes.JSON)
    private String templateData; // JSON string

    @Column(name = "locale")
    private String locale;
}
