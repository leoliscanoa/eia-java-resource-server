package com.lliscano.eia.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectUpdateRequestDTO implements Serializable {

    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    private String name;

    private String description;

    @Size(min = 2, max = 100, message = "Sector must be between 2 and 100 characters")
    private String sector;

    @Size(max = 100, message = "Typology must not exceed 100 characters")
    private String typology;

    private String stage;

    private LocalDate startDate;

    private LocalDate estimatedEndDate;

    private BigDecimal budgetEstimate;

    private String currency;

    private String status;
}
