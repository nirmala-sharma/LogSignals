package com.nirmala.logsense.detector;

import com.nirmala.logsense.config.AnomalyDetectionConfig;
import com.nirmala.logsense.model.AggregationKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Slf4j  // This automatically creates a `log` object for this class
@Component
public class AnomalyDetector {
    public AnomalyDetector() {
    }

    public Map<String, Map<String, List<Instant>>> detect(
            Map<AggregationKey, Integer> errorCount,
            AnomalyDetectionConfig config) {

        Map<String, Map<String, List<Instant>>> anomalies = new HashMap<>();

        int windowSize = config.getWindowSize();
        double k = config.getThreshold();
        double minStdDev = config.getMinimumStandardDeviation();
        int minSamples = config.getMinimumSamples();

        // Group by service + errorCode first
        Map<String, Map<String, Map<Instant, Integer>>> grouped = new HashMap<>();

        for (Map.Entry<AggregationKey, Integer> entry : errorCount.entrySet()) {
            AggregationKey key = entry.getKey();
            grouped
                    .computeIfAbsent(key.getService(), s -> new HashMap<>())
                    .computeIfAbsent(key.getErrorCode(), e -> new HashMap<>())
                    .put(key.getMinuteBucket(), entry.getValue());
        }

        // Run detection on grouped data
        for (Map.Entry<String, Map<String, Map<Instant, Integer>>> serviceEntry : grouped.entrySet()) {
            String service = serviceEntry.getKey();

            for (Map.Entry<String, Map<Instant, Integer>> errorEntry : serviceEntry.getValue().entrySet()) {
                String errorCode = errorEntry.getKey();
                Map<Instant, Integer> minuteCounts = errorEntry.getValue();

                List<Instant> sortedMinutes = new ArrayList<>(minuteCounts.keySet());
                Collections.sort(sortedMinutes);

                Deque<Integer> rollingWindow = new ArrayDeque<>();
                List<Instant> anomalyMinutes = new ArrayList<>();

                for (Instant minute : sortedMinutes) {
                    int currentCount = minuteCounts.get(minute);

                    if (rollingWindow.size() >= minSamples) {
                        double mean = calculateMean(rollingWindow);
                        double stdDev = Math.max(calculateStdDev(rollingWindow, mean), minStdDev);
                        double dynamicThreshold = mean + (k * stdDev);

                        if (currentCount >= dynamicThreshold) {
                            log.info("ANOMALY detected at {} | service={} | errorCode={} | count={} | mean={} | stdDev={} | threshold={}",
                                    minute, service, errorCode, currentCount, mean, stdDev, dynamicThreshold);
                            anomalyMinutes.add(minute);
                        }
                    }

                    rollingWindow.addLast(currentCount);
                    if (rollingWindow.size() > windowSize) {
                        rollingWindow.removeFirst();
                    }
                }

                if (!anomalyMinutes.isEmpty()) {
                    anomalies
                            .computeIfAbsent(service, s -> new HashMap<>())
                            .put(errorCode, anomalyMinutes);
                }
            }
        }
        return anomalies;
    }
    private double calculateMean(Deque<Integer> window) {
        double sum = 0;
        for (int value : window) {
            sum += value;
        }
        return sum / window.size();
    }

    private double calculateStdDev(Deque<Integer> window, double mean) {
        double sum = 0;
        for (int value : window) {
            double diff = value - mean;
            sum += diff * diff;
        }
        return Math.sqrt(sum / window.size());
    }
}