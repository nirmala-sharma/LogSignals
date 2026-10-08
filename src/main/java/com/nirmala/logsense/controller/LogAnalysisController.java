package com.nirmala.logsense.controller;

import com.nirmala.logsense.dto.LiveIngestRequestDTO;
import com.nirmala.logsense.dto.LiveIngestResponseDTO;
import com.nirmala.logsense.dto.LogAnalysisResponseDTO;
import com.nirmala.logsense.dto.StreamIngestResponseDTO;
import com.nirmala.logsense.service.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/logs")
@SecurityRequirement(name = "apiKeyAuth")
public class LogAnalysisController {

    private final LogAnalysisService logService;
    private final LiveLogIngestionService liveLogIngestionService;
    private final ApiKeyService apiKeyService;
    private final LogPersistenceService logPersistenceService;
    private final LogStreamIngestionService logStreamIngestionService;
    public LogAnalysisController(
            LogAnalysisService logService,
            LiveLogIngestionService liveLogIngestionService, ApiKeyService apiKeyService,
            LogPersistenceService logPersistenceService, LogStreamIngestionService logStreamIngestionService) {
        this.logService = logService;
        this.liveLogIngestionService = liveLogIngestionService;
        this.apiKeyService = apiKeyService;
        this.logPersistenceService = logPersistenceService;
        this.logStreamIngestionService = logStreamIngestionService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LogAnalysisResponseDTO analyzeLogs(
            @Parameter(hidden = true) @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestPart("file") MultipartFile file
    ) {
        Long applicationId = apiKeyService.getApplicationIdFromApiKey(apiKey);
        return logService.runAnalysis(applicationId, file);
    }

    @PostMapping(value = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LiveIngestResponseDTO ingestLog(
            @Parameter(hidden = true) @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Valid @RequestBody LiveIngestRequestDTO request
    ) {
        Long applicationId = apiKeyService.getApplicationIdFromApiKey(apiKey);
        return liveLogIngestionService.ingest(applicationId, request);
    }

    /**
     * Streaming ingestion: the client keeps one connection open and send one JSON log per line
     * Each line is processed as it arrives; a summary is returned when the client ends the stream.
     */
    @PostMapping(value = "/stream",consumes = {"application/x-ndjson",MediaType.TEXT_PLAIN_VALUE,MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public StreamIngestResponseDTO streamLogs(
            @Parameter(hidden = true) @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            HttpServletRequest request
    ) throws IOException {
        Long applicationId = apiKeyService.getApplicationIdFromApiKey(apiKey);
        return logStreamIngestionService.ingestStream(applicationId, request.getInputStream());
    }
}
