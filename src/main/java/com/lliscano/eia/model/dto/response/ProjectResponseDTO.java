package com.lliscano.eia.model.dto.response;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponseDTO implements Serializable {

    private String uuid;
    private String code;
    private String name;
    private String description;
    private String sector;
    private String typology;
    private String stage;
    private String status;
    private LocalDate startDate;
    private LocalDate estimatedEndDate;
    private BigDecimal budgetEstimate;
    private String currency;
    private int directInfluenceCount;
    private int indirectInfluenceCount;
    private String userRole;
    private LocalDateTime createdAt;
}
