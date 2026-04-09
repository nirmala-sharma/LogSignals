package com.nirmala.logsense.service;

import com.nirmala.logsense.Aggregator;
import com.nirmala.logsense.AnomalyDetector;
import com.nirmala.logsense.IncidentCorrelator;
import com.nirmala.logsense.IncidentExplainer;
import com.nirmala.logsense.model.Incident;
import com.nirmala.logsense.model.LogModel;
import com.nirmala.logsense.parser.LogParser;
import com.sun.tools.javac.Main;
import org.springframework.stereotype.Service;

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
    public String runAnalysis() {
        Map<Instant, Integer> errorsPerMinute = new HashMap<>();
        Aggregator aggregator = new Aggregator();
        AnomalyDetector detector = new AnomalyDetector();
        int windowSize = 1;
        double minimumStandardDeviation = 1.0;
        int minimumSamples = 1;

        try (InputStream is = Main.class
                .getClassLoader()
                .getResourceAsStream("logs/app.log")) {

            if (is == null) {
                throw new RuntimeException("Log file not found in resources");
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // log parsing
                    LogModel log = LogParser.parse(line);

                    // log aggregation
                    aggregator.add(log);

                } catch (Exception e) {
                    System.err.println("Invalid log line: " + line);
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
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return "Done";
    }
}

