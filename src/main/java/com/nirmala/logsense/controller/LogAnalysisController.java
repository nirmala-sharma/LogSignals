package com.nirmala.logsense.controller;

import com.nirmala.logsense.dto.LogAnalysisResponseDTO;
import com.nirmala.logsense.service.LogAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
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
