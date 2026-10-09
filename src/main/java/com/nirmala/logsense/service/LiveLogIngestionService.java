package com.nirmala.logsense.service;

import com.nirmala.logsense.aggregator.Aggregator;
import com.nirmala.logsense.config.AnomalyDetectionConfig;
import com.nirmala.logsense.correlator.IncidentCorrelator;
import com.nirmala.logsense.detector.AnomalyDetector;
import com.nirmala.logsense.dto.IncidentResponseDTO;
import com.nirmala.logsense.dto.LiveIngestRequestDTO;
import com.nirmala.logsense.dto.LiveIngestResponseDTO;
import com.nirmala.logsense.explainer.IncidentExplainer;
import com.nirmala.logsense.model.AggregationKey;
import com.nirmala.logsense.model.Incident;
import com.nirmala.logsense.model.LogModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles live (one log at a time) ingestion.
 *
 * <p>Each application gets its own isolated in-memory state, so logs from one
 * tenant can never influence another tenant's detection or trigger alerts to
 * another tenant's owner. State is bounded: minute buckets older than the
 * retention window are evicted, and every anomaly (service + errorCode + minute)
 * is persisted and alerted exactly once.</p>
 */
@Slf4j
@Service
public class LiveLogIngestionService {

    /** Per-application live state. Access is guarded by synchronizing on the instance. */
    private static final class AppLiveState {
        private final Aggregator aggregator = new Aggregator();
        private final Set<AggregationKey> alreadyReported = new HashSet<>();
        private Instant newestMinute;
    }

    private final Map<Long, AppLiveState> stateByApplication = new ConcurrentHashMap<>();

    private final AnomalyDetector detector;
    private final IncidentCorrelator correlator;
    private final IncidentExplainer explainer;
    private final AnomalyDetectionConfig config;
    private final LogPersistenceService logPersistenceService;
    private final AlertNotificationService alertNotificationService;
    private final long retentionMinutes;

    public LiveLogIngestionService(
            AnomalyDetector detector,
            IncidentCorrelator correlator,
            IncidentExplainer explainer,
            AnomalyDetectionConfig config,
            LogPersistenceService logPersistenceService,
            AlertNotificationService alertNotificationService,
            @Value("${logsense.live.retentionMinutes:60}") long retentionMinutes) {
        this.detector = detector;
        this.correlator = correlator;
        this.explainer = explainer;
        this.config = config;
        this.logPersistenceService = logPersistenceService;
        this.alertNotificationService = alertNotificationService;
        this.retentionMinutes = retentionMinutes;
    }

    /** Used by POST /api/logs/ingest: converts the JSON request and runs the pipeline. */
    public LiveIngestResponseDTO ingest(Long applicationId, LiveIngestRequestDTO request) {
        return ingest(applicationId, request.toLogModel());
    }

    /** Runs one already-parsed log through the live pipeline. Also used by the stream endpoint. */
    public LiveIngestResponseDTO ingest(Long applicationId, LogModel logModel) {
        AppLiveState state = stateByApplication.computeIfAbsent(applicationId, id -> new AppLiveState());

        Instant minuteBucket = logModel.getTimestamp().truncatedTo(ChronoUnit.MINUTES);

        LiveIngestResponseDTO response;
        Map<String, Map<String, List<Instant>>> newAnomalies;

        // Lock only this application's state: different applications ingest in parallel.
        synchronized (state) {
            Aggregator aggregator = state.aggregator;
            aggregator.add(logModel);
            aggregator.setTotalLines(aggregator.getTotalLines() + 1);

            evictExpired(state, minuteBucket);

            Map<String, Map<String, List<Instant>>> anomalies =
                    detector.detect(aggregator.getErrorCount(), config);

            Map<String, Map<String, List<Incident>>> incidents = correlator.group(anomalies);
            explainIncidents(incidents, aggregator);

            // Only anomalies we have not already stored/alerted for this application.
            newAnomalies = extractNewAnomalies(anomalies, state.alreadyReported);

            boolean anomalyDetected = isAnomalyForCurrentLog(
                    anomalies, logModel.getService(), logModel.getErrorCode(), minuteBucket);

            response = buildResponse(
                    aggregator.getTotalLines(), minuteBucket, anomalyDetected, anomalies, incidents);

            logPersistenceService.saveLiveIngestResult(applicationId, logModel, newAnomalies);

            log.info("Live log ingested. applicationId={}, service={}, errorCode={}, minute={}, anomalyDetected={}, newAnomalies={}",
                    applicationId, logModel.getService(), logModel.getErrorCode(),
                    minuteBucket, anomalyDetected, !newAnomalies.isEmpty());
        }

        // Send email outside the lock so a slow mail server does not block this app's stream.
        alertNotificationService.sendAlertsIfNeeded(applicationId, newAnomalies);

        return response;
    }

    /** Keeps only the last {@code retentionMinutes} of buckets (relative to the newest log seen). */
    private void evictExpired(AppLiveState state, Instant minuteBucket) {
        if (state.newestMinute == null || minuteBucket.isAfter(state.newestMinute)) {
            state.newestMinute = minuteBucket;
        }
        Instant cutoff = state.newestMinute.minus(retentionMinutes, ChronoUnit.MINUTES);
        state.aggregator.evictOlderThan(cutoff);
        state.alreadyReported.removeIf(key -> key.getMinuteBucket().isBefore(cutoff));
    }

    /**
     * Returns the anomalies not reported before and records them as reported.
     */
    private Map<String, Map<String, List<Instant>>> extractNewAnomalies(
            Map<String, Map<String, List<Instant>>> anomalies,
            Set<AggregationKey> alreadyReported) {

        Map<String, Map<String, List<Instant>>> fresh = new HashMap<>();

        for (Map.Entry<String, Map<String, List<Instant>>> serviceEntry : anomalies.entrySet()) {
            String service = serviceEntry.getKey();
            for (Map.Entry<String, List<Instant>> errorEntry : serviceEntry.getValue().entrySet()) {
                String errorCode = errorEntry.getKey();
                for (Instant minute : errorEntry.getValue()) {
                    AggregationKey key = new AggregationKey(service, errorCode, minute);
                    if (alreadyReported.add(key)) {
                        fresh.computeIfAbsent(service, s -> new HashMap<>())
                                .computeIfAbsent(errorCode, e -> new ArrayList<>())
                                .add(minute);
                    }
                }
            }
        }
        return fresh;
    }

    private void explainIncidents(Map<String, Map<String, List<Incident>>> incidents, Aggregator aggregator) {
        for (Map.Entry<String, Map<String, List<Incident>>> serviceEntry : incidents.entrySet()) {
            String service = serviceEntry.getKey();

            for (Map.Entry<String, List<Incident>> errorEntry : serviceEntry.getValue().entrySet()) {
                String errorCode = errorEntry.getKey();

                for (Incident incident : errorEntry.getValue()) {
                    explainer.explainIncident(service, errorCode, incident, aggregator.getErrorLogs());
                }
            }
        }
    }

    private boolean isAnomalyForCurrentLog(
            Map<String, Map<String, List<Instant>>> anomalies,
            String service,
            String errorCode,
            Instant minuteBucket) {

        Map<String, List<Instant>> anomaliesByErrorCode = anomalies.get(service);
        if (anomaliesByErrorCode == null) {
            return false;
        }
        List<Instant> minutes = anomaliesByErrorCode.get(errorCode);
        return minutes != null && minutes.contains(minuteBucket);
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
                    incidentResponses.add(new IncidentResponseDTO(incident.getStart(), incident.getExplanation()));
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
