package com.nirmala.logsense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private Long applicationId;
    private String applicationName;
    private String apiKey;
    private String message;
}
