package com.nirmala.logsense;

import com.nirmala.logsense.model.LogModel;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Aggregator{

        // stores: minute -> number of ERROR logs
        private final Map<Instant, Integer> errorsPerMinute = new HashMap<>();
        // stores: minute -> number of ERROR logs
        private final Map<Instant, List<LogModel>> errorLogsByMinute = new HashMap<>();


    public void add(LogModel log) {

            // convert timestamp to minute bucket (e.g. 10:00:25 -> 10:00:00)
            Instant minuteBucket = log.getTimestamp().truncatedTo(ChronoUnit.MINUTES);

            int current = errorsPerMinute.getOrDefault(minuteBucket, 0);

            // increase error count for that minute
            errorsPerMinute.put(minuteBucket, current + 1);

         // store the actual log inside that minute
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

