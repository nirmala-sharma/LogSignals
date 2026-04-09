package com.nirmala.logsense.controller;

import com.nirmala.logsense.service.LogAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LogAnalysisController {

    private final LogAnalysisService logService;

    public LogAnalysisController(LogAnalysisService logService) {
        this.logService = logService;
    }

    @GetMapping("/analyze")
    public Map<String, String> analyzeLogs() {
        Map<String, String> response = new HashMap<>();
        response.put("message", logService.runAnalysis());
        return response;
    }
}
