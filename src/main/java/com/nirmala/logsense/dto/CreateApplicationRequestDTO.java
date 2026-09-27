package com.nirmala.logsense.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * The owner is taken from the caller's access token, never from the request body,
 * so a user can only create applications for themselves.
 */
@Data
public class CreateApplicationRequestDTO {
    @NotBlank(message = "name is required")
    private String name;
    private String description;
    private String apiKeyName;
}
