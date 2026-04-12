package com.nirmala.logsense.controller;

import com.nirmala.logsense.dtos.LogAnalysisResponseDTO;
import com.nirmala.logsense.service.LogAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogAnalysisController {

    private final LogAnalysisService logService;

    public LogAnalysisController(LogAnalysisService logService) {
        this.logService = logService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LogAnalysisResponseDTO analyzeLogs(@RequestPart("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
       return logService.runAnalysis(file);

    }
}
