package com.nirmala.logsense;

import java.time.Instant;
import java.util.*;

public class AnomalyDetector {
    public AnomalyDetector() {}

    public Map<String, Map<String, List<Instant>>> detect(
            Map<String, Map<String, Map<Instant, Integer>>> errorsPerServiceAndCode,
            int winSize,
            double minimumStandardDeviation,
            int minimumSamples) {

        Map<String, Map<String, List<Instant>>> anomalies = new HashMap<>();

        int windowSize = winSize;
        double k = 2.0;
        double minStdDev = minimumStandardDeviation;
        int minSamples = minimumSamples;

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
                            System.out.println(
                                    "ANOMALY detected at " + minute +
                                            " | service = " + service +
                                            " | errorCode = " + errorCode +
                                            " | error_count = " + currentCount +
                                            " | rolling_mean = " + mean +
                                            " | rolling_stdDev = " + stdDev +
                                            " | dynamic_threshold = " + dynamicThreshold
                            );
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