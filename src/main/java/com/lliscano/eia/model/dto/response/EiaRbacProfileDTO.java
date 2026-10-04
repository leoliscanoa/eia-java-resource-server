package com.lliscano.eia.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EiaRbacProfileDTO implements Serializable {
    private boolean isEiaAdmin;
    private List<String> assignedProjectIds;
}
