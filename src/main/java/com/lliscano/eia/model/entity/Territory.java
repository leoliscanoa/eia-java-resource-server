package com.lliscano.eia.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "territory", schema = "eia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Territory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, length = 36, unique = true)
    private String uuid;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "municipality", nullable = false, length = 100)
    private String municipality;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "geometry_geojson", columnDefinition = "jsonb")
    private String geometryGeojson;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @UpdateTimestamp
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @PrePersist
    public void prePersist() {
        if (this.uuid == null || this.uuid.isBlank()) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
