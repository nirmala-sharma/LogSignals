package com.nirmala.logsense.dto;

import com.nirmala.logsense.model.LogModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveIngestRequestDTO {

    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    @NotBlank(message = "level is required")
    private String level;

    @NotBlank(message = "service is required")
    private String service;

    private String errorCode;

    @NotBlank(message = "message is required")
    private String message;

    public LogModel toLogModel() {
        return new LogModel(timestamp, level, service, errorCode, message);
    }
}
