package com.lliscano.eia.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lliscano.commons.entities.AuditingBase;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "project_role", schema = "eia",
        uniqueConstraints = {@UniqueConstraint(name = "uq_project_role_code", columnNames = {"code"})},
        indexes = {
                @Index(name = "ix_project_role_code", columnList = "code"),
                @Index(name = "ix_project_role_active", columnList = "is_active")
        })
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectRole extends AuditingBase {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_lead_role", nullable = false)
    @Builder.Default
    private boolean isLeadRole = false;

    @Column(name = "is_default_member", nullable = false)
    @Builder.Default
    private boolean isDefaultMember = false;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
