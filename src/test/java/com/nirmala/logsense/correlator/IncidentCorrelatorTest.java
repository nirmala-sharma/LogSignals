package com.nirmala.logsense.correlator;

import com.nirmala.logsense.model.Incident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IncidentCorrelatorTest {

    private IncidentCorrelator correlator;

    @BeforeEach
    void setUp() {
        correlator = new IncidentCorrelator();
    }

    @Test
    void shouldReturnEmptyWhenNoAnomaliesExist() {
        // Arrange
        Map<String, Map<String, List<Instant>>> anomalies = new HashMap<>();

        // Act
        Map<String, Map<String, List<Incident>>> result =
                correlator.group(anomalies);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldGroupConsecutiveMinutesIntoOneIncident() {
        // Arrange — 3 consecutive anomaly minutes
        Instant t1 = Instant.parse("2026-04-15T10:00:00Z");
        Instant t2 = Instant.parse("2026-04-15T10:01:00Z");
        Instant t3 = Instant.parse("2026-04-15T10:02:00Z");

        Map<String, Map<String, List<Instant>>> anomalies = new HashMap<>();
        anomalies
                .computeIfAbsent("auth-service", s -> new HashMap<>())
                .put("ERR_500", List.of(t1, t2, t3));

        // Act
        Map<String, Map<String, List<Incident>>> result =
                correlator.group(anomalies);

        // Assert — should be ONE incident spanning t1 to t3
        List<Incident> incidents = result.get("auth-service").get("ERR_500");
        assertEquals(1, incidents.size());
        assertEquals(t1, incidents.get(0).getStart());
        assertEquals(t3, incidents.get(0).getEnd());
    }

    @Test
    void shouldCreateTwoIncidentsWhenMinutesAreNotConsecutive() {
        // Arrange — gap between t2 and t3
        Instant t1 = Instant.parse("2026-04-15T10:00:00Z");
        Instant t2 = Instant.parse("2026-04-15T10:01:00Z");
        Instant t3 = Instant.parse("2026-04-15T10:05:00Z"); // gap!

        Map<String, Map<String, List<Instant>>> anomalies = new HashMap<>();
        anomalies
                .computeIfAbsent("auth-service", s -> new HashMap<>())
                .put("ERR_500", List.of(t1, t2, t3));

        // Act
        Map<String, Map<String, List<Incident>>> result =
                correlator.group(anomalies);

        // Assert — should be TWO separate incidents
        List<Incident> incidents = result.get("auth-service").get("ERR_500");
        assertEquals(2, incidents.size());
    }

    @Test
    void shouldReturnEmptyWhenAnomalyMapIsNull() {
        // Act
        Map<String, Map<String, List<Incident>>> result =
                correlator.group(null);

        // Assert
        assertTrue(result.isEmpty());
    }
}
