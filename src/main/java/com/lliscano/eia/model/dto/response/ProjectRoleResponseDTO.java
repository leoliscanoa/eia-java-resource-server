package com.lliscano.eia.model.dto.response;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRoleResponseDTO implements Serializable {

    private String code;
    private String name;
    private String description;
    private boolean isLeadRole;
    private boolean isDefaultMember;
    private boolean isActive;
    private Instant createdAt;
}
