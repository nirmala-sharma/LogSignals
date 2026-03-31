package com.nirmala.logsense;

import java.time.Instant;
import java.util.*;

public class AnomalyDetector {

    public AnomalyDetector() {}

    public ArrayList<Instant> detect(Map<Instant, Integer> errorsPerMinute, int winSize, double minimumStandardDeviation, int minimumSamples) {

        ArrayList<Instant> anomalyMinutes = new ArrayList<>();

        List<Instant> sortedMinutes = new ArrayList<>(errorsPerMinute.keySet());
        Collections.sort(sortedMinutes);

        Deque<Integer> rollingWindow = new ArrayDeque<>();   // Stores only the last N minutes

        int windowSize = winSize;
        double k = 2.0;           // sensitivity multiplier
        double minStdDev = minimumStandardDeviation;   // avoids overly tight threshold
        int minSamples = minimumSamples;       // warm-up

        for (Instant minute : sortedMinutes) {
            int currentCount = errorsPerMinute.get(minute);

            if (rollingWindow.size() >= minSamples) {
                double mean = calculateMean(rollingWindow);
                double stdDev = calculateStdDev(rollingWindow, mean);
                stdDev = Math.max(stdDev, minStdDev);

                double dynamicThreshold = mean + (k * stdDev);

                if (currentCount > dynamicThreshold) {
                    System.out.println(
                            "ANOMALY detected at " + minute +
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
        return anomalyMinutes;
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
