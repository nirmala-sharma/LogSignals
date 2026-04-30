package com.nirmala.logsense.controller;

import com.nirmala.logsense.dto.LiveIngestRequestDTO;
import com.nirmala.logsense.dto.LiveIngestResponseDTO;
import com.nirmala.logsense.dto.LogAnalysisResponseDTO;
import com.nirmala.logsense.service.ApiKeyService;
import com.nirmala.logsense.service.LogAnalysisService;
import com.nirmala.logsense.service.LiveLogIngestionService;
import com.nirmala.logsense.service.LogPersistenceService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/logs")
public class LogAnalysisController {

    private final LogAnalysisService logService;
    private final LiveLogIngestionService liveLogIngestionService;
    private final ApiKeyService apiKeyService;
    private final LogPersistenceService logPersistenceService;

    public LogAnalysisController(
            LogAnalysisService logService,
            LiveLogIngestionService liveLogIngestionService, ApiKeyService apiKeyService,
            LogPersistenceService logPersistenceService) {
        this.logService = logService;
        this.liveLogIngestionService = liveLogIngestionService;
        this.apiKeyService = apiKeyService;
        this.logPersistenceService = logPersistenceService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LogAnalysisResponseDTO analyzeLogs(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestPart("file") MultipartFile file
    ) {
        Long applicationId = apiKeyService.getApplicationIdFromApiKey(apiKey);
        return logService.runAnalysis(applicationId, file);
    }

    @PostMapping(value = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LiveIngestResponseDTO ingestLog(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody LiveIngestRequestDTO request
    ) {
        Long applicationId = apiKeyService.getApplicationIdFromApiKey(apiKey);
        return liveLogIngestionService.ingest(applicationId, request);
    }
}
