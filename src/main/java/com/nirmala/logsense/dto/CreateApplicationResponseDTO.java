package com.nirmala.logsense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateApplicationResponseDTO {
    private Long applicationId;
    private String applicationName;
    private String apiKey;
    private String keyPrefix;
}
