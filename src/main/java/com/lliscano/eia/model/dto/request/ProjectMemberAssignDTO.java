package com.lliscano.eia.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberAssignDTO implements Serializable {

    @NotBlank(message = "User UUID is required")
    private String userUuid;

    @NotBlank(message = "Project role is required")
    private String projectRole;
}
