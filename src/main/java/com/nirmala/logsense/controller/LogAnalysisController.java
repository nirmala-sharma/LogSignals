package com.nirmala.logsense.controller;

import com.nirmala.logsense.dto.LiveIngestRequestDTO;
import com.nirmala.logsense.dto.LiveIngestResponseDTO;
import com.nirmala.logsense.dto.LogAnalysisResponseDTO;
import com.nirmala.logsense.service.LogAnalysisService;
import com.nirmala.logsense.service.LiveLogIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/logs")
public class LogAnalysisController {

    private final LogAnalysisService logService;
    private final LiveLogIngestionService liveLogIngestionService;

    public LogAnalysisController(
            LogAnalysisService logService,
            LiveLogIngestionService liveLogIngestionService) {
        this.logService = logService;
        this.liveLogIngestionService = liveLogIngestionService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LogAnalysisResponseDTO analyzeLogs(@RequestPart("file") MultipartFile file) {
        return logService.runAnalysis(file);
    }

    @PostMapping(value = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LiveIngestResponseDTO ingestLog(@Valid @RequestBody LiveIngestRequestDTO request) {
        return liveLogIngestionService.ingest(request);
    }
}
