package com.nirmala.logsense;

import com.nirmala.logsense.model.LogModel;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Aggregator{

        private final Map<Instant, Integer> errorsPerMinute = new HashMap<>();

        private final Map<Instant, List<LogModel>> errorLogsByMinute = new HashMap<>();


    public void add(LogModel log) {
            Instant minuteBucket = log.getTimestamp().truncatedTo(ChronoUnit.MINUTES);

            int current = errorsPerMinute.getOrDefault(minuteBucket, 0);

            errorsPerMinute.put(minuteBucket, current + 1);

        // store per-minute logs for explanation
         List<LogModel> list = errorLogsByMinute.get(minuteBucket);
        if (list == null) {
            list = new ArrayList<>();
            errorLogsByMinute.put(minuteBucket, list);
        }
        list.add(log);
    }

        public Map<Instant, Integer> getErrorsPerMinute() {
            return errorsPerMinute;
        }
        public Map<Instant, List<LogModel>> getErrorLogsByMinute() {
        return errorLogsByMinute;
    }

}

