package com.nirmala.logsense.detector;

import com.nirmala.logsense.config.AnomalyDetectionConfig;
import com.nirmala.logsense.model.AggregationKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnomalyDetectorTest {

    private AnomalyDetector detector;
    private AnomalyDetectionConfig config;

    @BeforeEach
    void setUp() {
        detector = new AnomalyDetector();

        // fake config — use simple values for testing
        config = new AnomalyDetectionConfig();
        config.setWindowSize(3);
        config.setMinimumSamples(3);
        config.setMinimumStandardDeviation(1.0);
        config.setThreshold(2.0);
    }

    @Test
    void shouldReturnEmptyWhenNoErrorsExist() {
        // Arrange — empty input
        Map<AggregationKey, Integer> errorCount = new HashMap<>();

        // Act — run detection
        Map<String, Map<String, List<Instant>>> result =
                detector.detect(errorCount, config);

        // Assert — should be empty
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDetectAnomalyWhenCountSpikeOccurs() {
        // Arrange — simulate normal counts then a spike
        Map<AggregationKey, Integer> errorCount = new HashMap<>();

        Instant t1 = Instant.parse("2026-04-15T10:00:00Z");
        Instant t2 = Instant.parse("2026-04-15T10:01:00Z");
        Instant t3 = Instant.parse("2026-04-15T10:02:00Z");
        Instant t4 = Instant.parse("2026-04-15T10:03:00Z"); // spike

        errorCount.put(new AggregationKey("auth-service", "ERR_500", t1), 5);
        errorCount.put(new AggregationKey("auth-service", "ERR_500", t2), 5);
        errorCount.put(new AggregationKey("auth-service", "ERR_500", t3), 5);
        errorCount.put(new AggregationKey("auth-service", "ERR_500", t4), 100); // spike!

        // Act
        Map<String, Map<String, List<Instant>>> result =
                detector.detect(errorCount, config);

        // Assert — anomaly should be detected
        assertTrue(result.containsKey("auth-service"));
        assertTrue(result.get("auth-service").containsKey("ERR_500"));
        assertFalse(result.get("auth-service").get("ERR_500").isEmpty());
    }

    @Test
    void shouldNotDetectAnomalyWhenCountsAreNormal() {
        // Arrange — all counts are similar, no spike
        Map<AggregationKey, Integer> errorCount = new HashMap<>();

        Instant t1 = Instant.parse("2026-04-15T10:00:00Z");
        Instant t2 = Instant.parse("2026-04-15T10:01:00Z");
        Instant t3 = Instant.parse("2026-04-15T10:02:00Z");
        Instant t4 = Instant.parse("2026-04-15T10:03:00Z");

        errorCount.put(new AggregationKey("auth-service", "ERR_500", t1), 5);
        errorCount.put(new AggregationKey("auth-service", "ERR_500", t2), 5);
        errorCount.put(new AggregationKey("auth-service", "ERR_500", t3), 6);
        errorCount.put(new AggregationKey("auth-service", "ERR_500", t4), 5);

        // Act
        Map<String, Map<String, List<Instant>>> result =
                detector.detect(errorCount, config);

        // Assert — no anomaly
        assertTrue(result.isEmpty());
    }
}
