package com.nirmala.logsense.controller;

import com.nirmala.logsense.dto.CreateApplicationRequestDTO;
import com.nirmala.logsense.dto.CreateApplicationResponseDTO;
import com.nirmala.logsense.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public CreateApplicationResponseDTO createApplication(
            @RequestBody CreateApplicationRequestDTO request
    ) {
        return applicationService.createApplication(request);
    }
}
