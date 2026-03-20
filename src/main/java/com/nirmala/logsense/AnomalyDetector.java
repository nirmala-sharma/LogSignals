package com.nirmala.logsense;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnomalyDetector {

    public AnomalyDetector() {}

    public ArrayList<Instant> detect(Map<Instant, Integer> errorsPerMinute) {

        ArrayList<Instant> anomalyMinutes = new ArrayList<>();

        List<Instant> sortedMinutes = new ArrayList<>(errorsPerMinute.keySet());
        Collections.sort(sortedMinutes);

        List<Integer> previousCounts = new ArrayList<>();

        for (Instant minute : sortedMinutes) {
            int currentCount = errorsPerMinute.get(minute);

            double average = 0;
            if (!previousCounts.isEmpty()) {
                int sum = 0;
                for (int count : previousCounts) {
                    sum += count;
                }
                average = (double) sum / previousCounts.size();
            }

            double dynamicThreshold = Math.max(3, average + 2);

            if (currentCount > dynamicThreshold) {
                System.out.println(
                        "ANOMALY detected at " + minute +
                                " | error_count = " + currentCount +
                                " | dynamic_threshold = " + dynamicThreshold
                );

                anomalyMinutes.add(minute);
            }

            previousCounts.add(currentCount);
        }

        return anomalyMinutes;
    }
}
