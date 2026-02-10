package com.nirmala.logsense;

import java.time.Instant;
import java.util.Map;

public class AnomalyDetector {

    private final int threshold;

    public AnomalyDetector(int threshold) {
        this.threshold = threshold;
    }

    public void detect(Map<Instant, Integer> errorsPerMinute) {
        for (Map.Entry<Instant, Integer> entry : errorsPerMinute.entrySet()) {
            Instant minute = entry.getKey();
            int count = entry.getValue();

            if (count > threshold) {
                System.out.println(
                        "ANOMALY detected at " + minute +
                                " | error_count = " + count
                );
            }
        }
    }
}
