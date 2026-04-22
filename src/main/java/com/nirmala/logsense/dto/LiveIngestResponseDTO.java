package com.nirmala.logsense.dto;

import com.nirmala.logsense.model.Incident;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class LiveIngestResponseDTO {

    private String status;
    private String message;
    private int totalLines;
    private boolean anomalyDetected;
    private Instant ingestedMinute;
    private Map<String, Map<String, List<Instant>>> anomalies;
    private Map<String, Map<String, List<Incident>>> incidents;
}
