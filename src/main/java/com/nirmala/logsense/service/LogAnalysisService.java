package com.nirmala.logsense.service;

import com.nirmala.logsense.Aggregator;
import com.nirmala.logsense.AnomalyDetector;
import com.nirmala.logsense.IncidentCorrelator;
import com.nirmala.logsense.IncidentExplainer;
import com.nirmala.logsense.dtos.LogAnalysisResponseDTO;
import com.nirmala.logsense.model.Incident;
import com.nirmala.logsense.model.LogModel;
import com.nirmala.logsense.parser.LogParser;
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
public class LogAnalysisService {
    public LogAnalysisResponseDTO runAnalysis(MultipartFile file) {
        LogAnalysisResponseDTO response = new LogAnalysisResponseDTO();
        Map<Instant, Integer> errorsPerMinute = new HashMap<>();
        Aggregator aggregator = new Aggregator();
        AnomalyDetector detector = new AnomalyDetector();
        int windowSize = 1;
        double minimumStandardDeviation = 1.0;
        int minimumSamples = 1;

        int totalLines=0;
        int invalidLines =0;
        try {

            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Uploaded log file is empty");
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
                        System.err.println("Invalid log line: " + line);
                    }
                }
            }
                // Anomaly Detection
                Map<String, Map<String, List<Instant>>> anomalyMap =
                        detector.detect(
                                aggregator.getErrorCount(),
                                windowSize,
                                minimumStandardDeviation,
                                minimumSamples
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
            response.setStatus("success");
            response.setMessage("Analysis completed successfully");
            response.setTotalLines(totalLines);
            response.setInvalidLines(invalidLines);
            response.setAnomalies(anomalyMap);
            response.setIncidents(incidentsByServiceAndErrorCode);

            return response;
            } catch (Exception e) {
            e.printStackTrace();
            response.setStatus("failed");
            response.setMessage("Analysis failed: " + e.getMessage());
            response.setTotalLines(totalLines);
            response.setInvalidLines(invalidLines);
            return response;
            }
        }
    }

