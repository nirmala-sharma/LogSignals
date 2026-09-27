package com.nirmala.logsense.service;

import com.nirmala.logsense.config.AnomalyDetectionConfig;
import com.nirmala.logsense.correlator.IncidentCorrelator;
import com.nirmala.logsense.detector.AnomalyDetector;
import com.nirmala.logsense.dto.LiveIngestRequestDTO;
import com.nirmala.logsense.dto.LiveIngestResponseDTO;
import com.nirmala.logsense.explainer.IncidentExplainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveLogIngestionServiceTest {

    private static final long APP_A = 1L;
    private static final long APP_B = 2L;

    @Mock private LogPersistenceService logPersistenceService;
    @Mock private AlertNotificationService alertNotificationService;

    private LiveLogIngestionService service;

    @BeforeEach
    void setUp() {
        AnomalyDetectionConfig config = new AnomalyDetectionConfig();
        config.setWindowSize(3);
        config.setMinimumSamples(3);
        config.setMinimumStandardDeviation(1.0);
        config.setThreshold(2.0);

        service = new LiveLogIngestionService(
                new AnomalyDetector(),
                new IncidentCorrelator(),
                new IncidentExplainer(),
                config,
                logPersistenceService,
                alertNotificationService,
                60
        );
    }

    private LiveIngestResponseDTO send(long appId, String time, String service, String errorCode) {
        return this.service.ingest(appId, new LiveIngestRequestDTO(
                Instant.parse(time), "ERROR", service, errorCode, "failure"));
    }

    /** Baseline of 1 error/minute for 3 minutes, then 3 errors in minute 4 (threshold = 1 + 2*1 = 3). */
    private LiveIngestResponseDTO sendSpike(long appId, String service) {
        send(appId, "2026-04-28T10:00:05Z", service, "ERR_500");
        send(appId, "2026-04-28T10:01:05Z", service, "ERR_500");
        send(appId, "2026-04-28T10:02:05Z", service, "ERR_500");
        send(appId, "2026-04-28T10:03:01Z", service, "ERR_500");
        send(appId, "2026-04-28T10:03:02Z", service, "ERR_500");
        return send(appId, "2026-04-28T10:03:03Z", service, "ERR_500");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Map<String, List<Instant>>>> capturedAlerts(long appId, int expectedCalls) {
        ArgumentCaptor<Map<String, Map<String, List<Instant>>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(alertNotificationService, times(expectedCalls)).sendAlertsIfNeeded(eq(appId), captor.capture());
        return captor.getAllValues();
    }

    @Test
    void detectsSpikeAndAlertsOnce() {
        LiveIngestResponseDTO response = sendSpike(APP_A, "payment-service");
        assertTrue(response.isAnomalyDetected());

        // more errors in the same anomalous minute must not re-alert
        send(APP_A, "2026-04-28T10:03:04Z", "payment-service", "ERR_500");
        send(APP_A, "2026-04-28T10:03:05Z", "payment-service", "ERR_500");

        List<Map<String, Map<String, List<Instant>>>> alerts = capturedAlerts(APP_A, 8);
        long nonEmptyAlerts = alerts.stream().filter(m -> !m.isEmpty()).count();
        assertEquals(1, nonEmptyAlerts, "anomaly should be alerted exactly once");
        assertEquals(List.of(Instant.parse("2026-04-28T10:03:00Z")),
                alerts.get(5).get("payment-service").get("ERR_500"));
    }

    @Test
    void applicationsAreIsolated() {
        sendSpike(APP_A, "payment-service");

        // App B's first log must not see App A's history or anomalies
        LiveIngestResponseDTO responseB = send(APP_B, "2026-04-28T10:03:10Z", "auth-service", "ERR_401");

        assertEquals(1, responseB.getTotalLines());
        assertFalse(responseB.isAnomalyDetected());
        assertTrue(responseB.getAnomalies().isEmpty());

        capturedAlerts(APP_B, 1).forEach(alert -> assertTrue(alert.isEmpty()));
        verify(logPersistenceService, never()).saveLiveIngestResult(eq(APP_B), any(),
                argThat(m -> m.containsKey("payment-service")));
    }

    @Test
    void oldBucketsAreEvicted() {
        sendSpike(APP_A, "payment-service");

        // two hours later: the old anomaly is outside the 60-minute retention window
        LiveIngestResponseDTO later = send(APP_A, "2026-04-28T12:10:00Z", "payment-service", "ERR_500");

        assertFalse(later.isAnomalyDetected());
        assertTrue(later.getAnomalies().isEmpty(), "evicted anomalies should not be reported again");
    }
}
