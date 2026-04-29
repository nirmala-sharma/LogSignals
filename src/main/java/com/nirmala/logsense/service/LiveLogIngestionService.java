package com.nirmala.logsense.service;

import com.nirmala.logsense.aggregator.Aggregator;
import com.nirmala.logsense.config.AnomalyDetectionConfig;
import com.nirmala.logsense.correlator.IncidentCorrelator;
import com.nirmala.logsense.detector.AnomalyDetector;
import com.nirmala.logsense.dto.IncidentResponseDTO;
import com.nirmala.logsense.dto.LiveIngestRequestDTO;
import com.nirmala.logsense.dto.LiveIngestResponseDTO;
import com.nirmala.logsense.explainer.IncidentExplainer;
import com.nirmala.logsense.model.Incident;
import com.nirmala.logsense.model.LogModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LiveLogIngestionService {

    private final Aggregator liveAggregator = new Aggregator();
    private final AnomalyDetector detector;
    private final IncidentCorrelator correlator;
    private final IncidentExplainer explainer;
    private final AnomalyDetectionConfig config;
    private final LogPersistenceService logPersistenceService;
    private final AlertNotificationService alertNotificationService;

    public LiveLogIngestionService(
            AnomalyDetector detector,
            IncidentCorrelator correlator,
            IncidentExplainer explainer,
            AnomalyDetectionConfig config,
            LogPersistenceService logPersistenceService,
            AlertNotificationService alertNotificationService) {
        this.detector = detector;
        this.correlator = correlator;
        this.explainer = explainer;
        this.config = config;
        this.logPersistenceService = logPersistenceService;
        this.alertNotificationService = alertNotificationService;
    }
    public synchronized LiveIngestResponseDTO ingest(Long applicationId, LiveIngestRequestDTO request) {
        LogModel logModel = request.toLogModel();
        Instant minuteBucket = logModel.getTimestamp().truncatedTo(ChronoUnit.MINUTES);

        liveAggregator.add(logModel);
        liveAggregator.setTotalLines(liveAggregator.getTotalLines() + 1);

        Map<String, Map<String, List<Instant>>> anomalies =
                detector.detect(liveAggregator.getErrorCount(), config);

        Map<String, Map<String, List<Incident>>> incidents =
                correlator.group(anomalies);

        explainIncidents(incidents);

        boolean anomalyDetected = isAnomalyForCurrentLog(
                anomalies,
                logModel.getService(),
                logModel.getErrorCode(),
                minuteBucket
        );

        LiveIngestResponseDTO response = buildResponse(
                liveAggregator.getTotalLines(),
                minuteBucket,
                anomalyDetected,
                anomalies,
                incidents
        );

        logPersistenceService.saveLiveIngestResult(
                applicationId,
                logModel,
                anomalies
        );
        alertNotificationService.sendAlertsIfNeeded(applicationId, anomalies);
        log.info("Live log ingested. service={}, errorCode={}, minute={}, anomalyDetected={}",
                logModel.getService(), logModel.getErrorCode(), minuteBucket, anomalyDetected);

        return response;
    }


    private void explainIncidents(Map<String, Map<String, List<Incident>>> incidents) {
        for (Map.Entry<String, Map<String, List<Incident>>> serviceEntry : incidents.entrySet()) {
            String service = serviceEntry.getKey();

            for (Map.Entry<String, List<Incident>> errorEntry : serviceEntry.getValue().entrySet()) {
                String errorCode = errorEntry.getKey();

                for (Incident incident : errorEntry.getValue()) {
                    explainer.explainIncident(
                            service,
                            errorCode,
                            incident,
                            liveAggregator.getErrorLogs()
                    );
                }
            }
        }
    }

    private boolean isAnomalyForCurrentLog(
            Map<String, Map<String, List<Instant>>> anomalies,
            String service,
            String errorCode,
            Instant minuteBucket) {

        if (!anomalies.containsKey(service)) {
            return false;
        }

        Map<String, List<Instant>> anomaliesByErrorCode = anomalies.get(service);
        if (!anomaliesByErrorCode.containsKey(errorCode)) {
            return false;
        }

        return anomaliesByErrorCode.get(errorCode).contains(minuteBucket);
    }

    private LiveIngestResponseDTO buildResponse(
            int totalLines,
            Instant ingestedMinute,
            boolean anomalyDetected,
            Map<String, Map<String, List<Instant>>> anomalies,
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

        LiveIngestResponseDTO response = new LiveIngestResponseDTO();
        response.setStatus("success");
        response.setMessage("Log ingested successfully");
        response.setTotalLines(totalLines);
        response.setIngestedMinute(ingestedMinute);
        response.setAnomalyDetected(anomalyDetected);
        response.setAnomalies(anomalies);
        response.setIncidents(responseIncidents);
        return response;
    }
}
