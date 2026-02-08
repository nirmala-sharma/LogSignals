package com.nirmala.logsense;

import com.nirmala.logsense.model.LogModel;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class Aggregator{

        private final Map<Instant, Integer> errorsPerMinute = new HashMap<>();

        public void add(LogModel log) {
            Instant minuteBucket = log.getTimestamp().truncatedTo(ChronoUnit.MINUTES);

            int current = errorsPerMinute.getOrDefault(minuteBucket, 0);

            errorsPerMinute.put(minuteBucket, current + 1);
        }

        public Map<Instant, Integer> getErrorsPerMinute() {
            return errorsPerMinute;
        }
}

