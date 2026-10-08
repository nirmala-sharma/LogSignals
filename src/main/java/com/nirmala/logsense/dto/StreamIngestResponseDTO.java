package com.nirmala.logsense.dto;

import lombok.Data;

/**
 * Summary returned when a log stream (POST /api/logs/stream) ends
 */
@Data
public class StreamIngestResponseDTO {
private String status;
private String message;
private int totalLines;
private int ingestedLines;
private int invalidLines;    // lines that were not valid log JSON (skipped)
private int anomalousLines;

}
