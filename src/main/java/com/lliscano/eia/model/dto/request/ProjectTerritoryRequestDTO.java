package com.lliscano.eia.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTerritoryRequestDTO implements Serializable {

    @NotBlank(message = "Territory UUID is required")
    private String territoryUuid;

    @NotBlank(message = "Influence type is required")
    private String influenceType;
}
