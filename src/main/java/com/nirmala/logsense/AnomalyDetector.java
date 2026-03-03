package com.nirmala.logsense;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class AnomalyDetector {

    private final int threshold;

    public AnomalyDetector(int threshold) {
        this.threshold = threshold;
    }

    public ArrayList<Instant> detect(Map<Instant, Integer> errorsPerMinute) {

        ArrayList<Instant> anomalyMinutes = new ArrayList<>();

        for (Map.Entry<Instant, Integer> entry : errorsPerMinute.entrySet()) {

            Instant minute = entry.getKey();
            int count = entry.getValue();

            if (count > threshold) {

                System.out.println(
                        "ANOMALY detected at " + minute +
                                " | error_count = " + count
                );

                anomalyMinutes.add(minute);
            }
        }

        // sort minutes in chronological order
        Collections.sort(anomalyMinutes);

        return anomalyMinutes;
    }
}
