package com.lliscano.eia.model.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProjectTerritoryId implements Serializable {
    private Long project;
    private Long territory;
}
