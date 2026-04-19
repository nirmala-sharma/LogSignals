package com.nirmala.logsense.service;

import com.nirmala.logsense.aggregator.Aggregator;
import com.nirmala.logsense.detector.AnomalyDetector;
import com.nirmala.logsense.correlator.IncidentCorrelator;
import com.nirmala.logsense.explainer.IncidentExplainer;
import com.nirmala.logsense.config.AnomalyDetectionConfig;
import com.nirmala.logsense.dto.LogAnalysisResponseDTO;
import com.nirmala.logsense.exception.EmptyLogFileException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j      // This automatically creates a `log` object for this class
public class LogAnalysisService {
    private final ApplicationContext context; // Spring's bean factory
    private final Aggregator aggregator;
    private final AnomalyDetector detector;
    private final IncidentCorrelator correlator;
    private final IncidentExplainer explainer;
    private final AnomalyDetectionConfig config;

    // Constructor injection
    public LogAnalysisService(Aggregator aggregator, AnomalyDetector detector, IncidentCorrelator correlator, IncidentExplainer explainer, AnomalyDetectionConfig config, ApplicationContext context) {
        this.context = context;
        this.aggregator = aggregator;
        this.detector = detector;
        this.correlator = correlator;
        this.explainer = explainer;
        this.config = config;
    }

    public LogAnalysisResponseDTO runAnalysis(MultipartFile file) {
        // Get a FRESH Aggregator for every request
        Aggregator aggregator = context.getBean(Aggregator.class);
        LogAnalysisResponseDTO response = new LogAnalysisResponseDTO();
        Map<Instant, Integer> errorsPerMinute = new HashMap<>();
        AnomalyDetector detector = new AnomalyDetector();

        int totalLines = 0;
        int invalidLines = 0;
        try {

            if (file == null || file.isEmpty()) {
                throw new EmptyLogFileException("Uploaded log file is empty");
            }
            try (InputStream is = file.getInputStream();
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    totalLines++;
                    try {
                        // log parsing
                        LogModel log = LogParser.parse(line);

                        // log aggregation
                        aggregator.add(log);

                    } catch (Exception e) {
                        invalidLines++;
                        // "warn" is correct here because it's not a crash, just a bad line
                        log.warn("Invalid log line skipped: {}", line);
                    }
                }
            }
            // Anomaly Detection
            Map<String, Map<String, List<Instant>>> anomalyMap =
                    detector.detect(
                            aggregator.getErrorCount(),
                            config
                    );

            // Incident Grouping / Correlation
            IncidentCorrelator correlator = new IncidentCorrelator();

            Map<String, Map<String, List<Incident>>> incidentsByServiceAndErrorCode =
                    correlator.group(anomalyMap);

            IncidentExplainer explainer = new IncidentExplainer();

            for (Map.Entry<String, Map<String, List<Incident>>> serviceEntry : incidentsByServiceAndErrorCode.entrySet()) {
                String service = serviceEntry.getKey();

                for (Map.Entry<String, List<Incident>> errorEntry : serviceEntry.getValue().entrySet()) {
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
            // BEFORE: no log at all when analysis succeeds
            // AFTER: log info so we can see in logs when analysis finishes
            log.info("Analysis completed successfully. totalLines={}, invalidLines={}", totalLines, invalidLines);
            response.setStatus("success");
            response.setMessage("Analysis completed successfully");
            response.setTotalLines(totalLines);
            response.setInvalidLines(invalidLines);
            response.setAnomalies(anomalyMap);
            response.setIncidents(incidentsByServiceAndErrorCode);

            return response;
        } catch (EmptyLogFileException e) {
            log.error("Empty file error: {}", e.getMessage());
            response.setStatus("failed");
            response.setMessage(e.getMessage());
            response.setTotalLines(totalLines);
            response.setInvalidLines(invalidLines);
            return response;

        } catch (Exception e) {
            log.error("Analysis failed: {}", e.getMessage(), e);
            response.setStatus("failed");
            response.setMessage("Analysis failed: " + e.getMessage());
            response.setTotalLines(totalLines);
            response.setInvalidLines(invalidLines);
            return response;
        }
    }
}

