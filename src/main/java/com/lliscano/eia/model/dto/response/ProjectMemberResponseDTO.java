package com.lliscano.eia.model.dto.response;

import com.lliscano.commons.dtos.AppUserSummaryDTO;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberResponseDTO implements Serializable {

    private String uuid;
    private String projectRole;
    private boolean isActive;
    private LocalDateTime createdAt;
    private AppUserSummaryDTO userSummary;
}
