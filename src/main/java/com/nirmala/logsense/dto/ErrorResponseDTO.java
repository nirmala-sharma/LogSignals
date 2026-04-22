package com.nirmala.logsense.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponseDTO {
    private String status;
    private String code;
    private String message;
}
