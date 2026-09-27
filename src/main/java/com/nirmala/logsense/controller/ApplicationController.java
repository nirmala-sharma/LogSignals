package com.nirmala.logsense.controller;

import com.nirmala.logsense.dto.CreateApplicationRequestDTO;
import com.nirmala.logsense.dto.CreateApplicationResponseDTO;
import com.nirmala.logsense.security.JwtTokenService;
import com.nirmala.logsense.service.ApplicationService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final JwtTokenService jwtTokenService;

    public ApplicationController(ApplicationService applicationService, JwtTokenService jwtTokenService) {
        this.applicationService = applicationService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateApplicationResponseDTO createApplication(
            @Parameter(hidden = true) // sent via the Swagger "Authorize" button
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CreateApplicationRequestDTO request) {
        Long userId = jwtTokenService.authenticate(authorization);
        return applicationService.createApplication(userId, request);
    }
}
