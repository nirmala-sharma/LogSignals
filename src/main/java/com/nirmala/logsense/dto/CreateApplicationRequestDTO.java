package com.nirmala.logsense.dto;

import lombok.Data;

@Data
public class CreateApplicationRequestDTO {
    private Long ownerUserId;
    private String name;
    private String description;
    private String apiKeyName;
}
