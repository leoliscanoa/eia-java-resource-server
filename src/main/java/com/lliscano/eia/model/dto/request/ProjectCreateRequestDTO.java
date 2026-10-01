package com.lliscano.eia.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCreateRequestDTO implements Serializable {

    @NotBlank(message = "Project code is required")
    @Size(min = 3, max = 50, message = "Code must be between 3 and 50 characters")
    private String code;

    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    private String name;

    private String description;

    @NotBlank(message = "Sector is required")
    @Size(min = 2, max = 100, message = "Sector must be between 2 and 100 characters")
    private String sector;

    @Size(max = 100, message = "Typology must not exceed 100 characters")
    private String typology;

    @Builder.Default
    private String stage = "PLANNING";

    private LocalDate startDate;

    private LocalDate estimatedEndDate;

    private BigDecimal budgetEstimate;

    @Builder.Default
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency = "COP";

    @Valid
    private List<ProjectTerritoryRequestDTO> territories;

    private String leadUserUuid;
}
