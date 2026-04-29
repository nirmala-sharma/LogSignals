package com.nirmala.logsense.service;

import com.nirmala.logsense.aggregator.Aggregator;
import com.nirmala.logsense.config.AnomalyDetectionConfig;
import com.nirmala.logsense.correlator.IncidentCorrelator;
import com.nirmala.logsense.detector.AnomalyDetector;
import com.nirmala.logsense.dto.IncidentResponseDTO;
import com.nirmala.logsense.dto.LogAnalysisResponseDTO;
import com.nirmala.logsense.exception.EmptyLogFileException;
import com.nirmala.logsense.explainer.IncidentExplainer;
import com.nirmala.logsense.model.Incident;
import com.nirmala.logsense.model.LogModel;
import com.nirmala.logsense.parser.LogParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LogAnalysisService {

    private final ApplicationContext context;
    private final AnomalyDetector detector;
    private final IncidentCorrelator correlator;
    private final IncidentExplainer explainer;
    private final AnomalyDetectionConfig config;
    private final LogPersistenceService logPersistenceService;
    private final AlertNotificationService alertNotificationService;

    public LogAnalysisService(
            ApplicationContext context,
            AnomalyDetector detector,
            IncidentCorrelator correlator,
            IncidentExplainer explainer,
            AnomalyDetectionConfig config,
            LogPersistenceService logPersistenceService,
            AlertNotificationService alertNotificationService) {
        this.context = context;
        this.detector = detector;
        this.correlator = correlator;
        this.explainer = explainer;
        this.config = config;
        this.logPersistenceService = logPersistenceService;
        this.alertNotificationService = alertNotificationService;
    }


    // ─────────────────────────────────────────
    // PUBLIC — orchestrates the full pipeline
    // ─────────────────────────────────────────

    public LogAnalysisResponseDTO runAnalysis(Long applicationId, MultipartFile file) {

        validateFile(file);

        Aggregator aggregator = context.getBean(Aggregator.class);
        parseAndAggregate(file, aggregator);

        Map<String, Map<String, List<Instant>>> anomalyMap =
                detectAnomalies(aggregator);

        Map<String, Map<String, List<Incident>>> incidents =
                correlateIncidents(anomalyMap);

        explainIncidents(incidents, aggregator);

        LogAnalysisResponseDTO response = buildSuccessResponse(
                aggregator.getTotalLines(),
                aggregator.getInvalidLines(),
                anomalyMap,
                incidents
        );

        logPersistenceService.saveAnalysisSummaryAndAnomalies(
                applicationId,
                response
        );
        alertNotificationService.sendAlertsIfNeeded(applicationId, anomalyMap);
        return response;
    }

    // ─────────────────────────────────────────
    // STEP 1 — Validate file
    // ─────────────────────────────────────────
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyLogFileException("Uploaded log file is empty");
        }
    }

    // ─────────────────────────────────────────
    // STEP 2 — Parse lines and aggregate
    // ─────────────────────────────────────────
    private void parseAndAggregate(MultipartFile file, Aggregator aggregator) {
        int totalLines = 0;
        int invalidLines = 0;

        try (InputStream is = file.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                try {
                    LogModel logModel = LogParser.parse(line);
                    aggregator.add(logModel);
                } catch (Exception e) {
                    invalidLines++;
                    log.warn("Invalid log line skipped: {}", line);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to read log file", e);
        }

        aggregator.setTotalLines(totalLines);
        aggregator.setInvalidLines(invalidLines);

        log.info("Parsing complete. totalLines={}, invalidLines={}",
                totalLines, invalidLines);
    }

    // ─────────────────────────────────────────
    // STEP 3 — Detect anomalies
    // ─────────────────────────────────────────
    private Map<String, Map<String, List<Instant>>> detectAnomalies(Aggregator aggregator) {
        Map<String, Map<String, List<Instant>>> anomalyMap =
                detector.detect(aggregator.getErrorCount(), config);
        log.info("Anomaly detection complete. anomaliesFound={}", anomalyMap.size());
        return anomalyMap;
    }

    // ─────────────────────────────────────────
    // STEP 4 — Correlate incidents
    // ─────────────────────────────────────────
    private Map<String, Map<String, List<Incident>>> correlateIncidents(
            Map<String, Map<String, List<Instant>>> anomalyMap) {
        Map<String, Map<String, List<Incident>>> incidents =
                correlator.group(anomalyMap);
        log.info("Incident correlation complete. incidentsFound={}", incidents.size());
        return incidents;
    }

    // ─────────────────────────────────────────
    // STEP 5 — Explain incidents
    // ─────────────────────────────────────────
    private void explainIncidents(
            Map<String, Map<String, List<Incident>>> incidents,
            Aggregator aggregator) {

        for (Map.Entry<String, Map<String, List<Incident>>> serviceEntry
                : incidents.entrySet()) {
            String service = serviceEntry.getKey();

            for (Map.Entry<String, List<Incident>> errorEntry
                    : serviceEntry.getValue().entrySet()) {
                String errorCode = errorEntry.getKey();

                for (Incident incident : errorEntry.getValue()) {
                    explainer.explainIncident(
                            service,
                            errorCode,
                            incident,
                            aggregator.getErrorLogs()
                    );
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // STEP 6 — Build responses
    // ─────────────────────────────────────────
    private LogAnalysisResponseDTO buildSuccessResponse(
            int totalLines,
            int invalidLines,
            Map<String, Map<String, List<Instant>>> anomalyMap,
            Map<String, Map<String, List<Incident>>> incidents) {

        Map<String, Map<String, List<IncidentResponseDTO>>> responseIncidents = new HashMap<>();

        for (Map.Entry<String, Map<String, List<Incident>>> serviceEntry : incidents.entrySet()) {
            Map<String, List<IncidentResponseDTO>> errorMap = new HashMap<>();

            for (Map.Entry<String, List<Incident>> errorEntry : serviceEntry.getValue().entrySet()) {
                List<IncidentResponseDTO> incidentResponses = new ArrayList<>();

                for (Incident incident : errorEntry.getValue()) {
                    incidentResponses.add(
                            new IncidentResponseDTO(
                                    incident.getStart(),
                                    incident.getExplanation()
                            )
                    );
                }

                errorMap.put(errorEntry.getKey(), incidentResponses);
            }

            responseIncidents.put(serviceEntry.getKey(), errorMap);
        }


        LogAnalysisResponseDTO response = new LogAnalysisResponseDTO();
        response.setStatus("SUCCESS");
        response.setMessage("Analysis completed successfully");
        response.setTotalLines(totalLines);
        response.setInvalidLines(invalidLines);
        response.setAnomalies(anomalyMap);
        response.setIncidents(responseIncidents);

        log.info("Analysis completed successfully. totalLines={}, invalidLines={}",
                totalLines, invalidLines);
        return response;
    }

    private LogAnalysisResponseDTO buildFailureResponse(String message) {
        LogAnalysisResponseDTO response = new LogAnalysisResponseDTO();
        response.setStatus("FAILED");
        response.setMessage(message);
        return response;
    }
}