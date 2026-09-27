package com.nirmala.logsense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;
    private String message;
}
