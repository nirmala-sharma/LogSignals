package com.nirmala.logsense.dto;

import lombok.Data;

import java.time.Instant;
@Data
public class IncidentResponseDTO {
    private Instant time;
    private String explanation;
    public IncidentResponseDTO(Instant time, String explanation) {
        this.time = time;
        this.explanation = explanation;
    }
}
