package com.nirmala.logsense.aggregator;

import com.nirmala.logsense.model.AggregationKey;
import com.nirmala.logsense.model.LogModel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")  // creates a NEW instance for every injection
public class Aggregator {

    // service -> errorCode -> minute -> count
    private final Map<AggregationKey, Integer> errorCount = new HashMap<>();

    // service -> errorCode -> minute -> logs
    //  Map<AggregationKey, List<LogModel>> — flat, clean, readable
    private final Map<AggregationKey, List<LogModel>> errorLogs = new HashMap<>();

    public void add(LogModel log) {
        if (log == null) {
            return;
        }
        Instant minuteBucket = log.getTimestamp().truncatedTo(ChronoUnit.MINUTES);

        AggregationKey key = new AggregationKey(
                log.getService(),
                log.getErrorCode(),
                minuteBucket
        );

        // count errors per key
        errorCount.merge(key, 1, Integer::sum);

        // store logs per key
        errorLogs.computeIfAbsent(key, k -> new ArrayList<>()).add(log);
    }


    public Map<AggregationKey, Integer> getErrorCount() {
        return errorCount;
    }

    public Map<AggregationKey, List<LogModel>> getErrorLogs() {
        return errorLogs;
    }

    public Map<Instant, Integer> getTotalErrorsPerMinute() {
        Map<Instant, Integer> totalErrorsPerMinute = new HashMap<>();
        for (Map.Entry<AggregationKey, Integer> entry : errorCount.entrySet()) {
            totalErrorsPerMinute.merge(
                    entry.getKey().getMinuteBucket(),
                    entry.getValue(),
                    Integer::sum
            );
        }
        return totalErrorsPerMinute;
    }
}

