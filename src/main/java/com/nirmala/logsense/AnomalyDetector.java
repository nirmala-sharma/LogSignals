package com.nirmala.logsense;

import com.nirmala.logsense.config.AnomalyDetectionConfig;
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
            Map<String, Map<String, Map<Instant, Integer>>> errorsPerServiceAndCode,
            AnomalyDetectionConfig config) {

        Map<String, Map<String, List<Instant>>> anomalies = new HashMap<>();

        int windowSize = config.getWindowSize();
        double k = config.getThreshold();
        double minStdDev = config.getMinimumStandardDeviation();
        int minSamples = config.getMinimumSamples();

        for (Map.Entry<String, Map<String, Map<Instant, Integer>>> serviceEntry : errorsPerServiceAndCode.entrySet()) {
            String service = serviceEntry.getKey();
            Map<String, Map<Instant, Integer>> errorCodeMap = serviceEntry.getValue();

            for (Map.Entry<String, Map<Instant, Integer>> errorEntry : errorCodeMap.entrySet()) {
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
                        double stdDev = calculateStdDev(rollingWindow, mean);
                        stdDev = Math.max(stdDev, minStdDev);

                        double dynamicThreshold = mean + (k * stdDev);

                        if (currentCount >= dynamicThreshold) {
                            // BEFORE: System.out.println("ANOMALY detected at ...")
                            // AFTER: log.info(...) — proper structured logging
                            log.info("ANOMALY detected at {} | service={} | errorCode={} | error_count={} | rolling_mean={} | rolling_stdDev={} | dynamic_threshold={}",
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