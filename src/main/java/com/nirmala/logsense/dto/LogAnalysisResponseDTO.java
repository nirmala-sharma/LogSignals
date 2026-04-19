package com.nirmala.logsense.dto;

import com.nirmala.logsense.model.Incident;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class LogAnalysisResponseDTO {

    private String status;
    private String message;
    private int totalLines;
    private int invalidLines;
    private Map<String, Map<String, List<Instant>>> anomalies;
    private Map<String, Map<String, List<Incident>>> incidents;

    public LogAnalysisResponseDTO() {
    }


}