package com.nirmala.logsense.aggregator;

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
    private final Map<String, Map<String, Map<Instant, Integer>>> errorCount = new HashMap<>();

    // service -> errorCode -> minute -> logs
    private final Map<String, Map<String, Map<Instant, List<LogModel>>>> errorLogs = new HashMap<>();

    public void add(LogModel log) {
        if (log == null) {
            return;
        }

        Instant minuteBucket = log.getTimestamp().truncatedTo(ChronoUnit.MINUTES);
        String service = log.getService();
        String errorCode = log.getErrorCode();

        // Count per service + errorCode + minute
        errorCount
                .computeIfAbsent(service, s -> new HashMap<>())
                .computeIfAbsent(errorCode, e -> new HashMap<>())
                .merge(minuteBucket, 1, Integer::sum);

        // Store logs per service + errorCode + minute
        errorLogs
                .computeIfAbsent(service, s -> new HashMap<>())
                .computeIfAbsent(errorCode, e -> new HashMap<>())
                .computeIfAbsent(minuteBucket, m -> new ArrayList<>())
                .add(log);
    }

    public Map<String, Map<String, Map<Instant, Integer>>> getErrorCount() {
        return errorCount;
    }

    public Map<String, Map<String, Map<Instant, List<LogModel>>>> getErrorLogs() {
        return errorLogs;
    }

    public Map<Instant, Integer> getTotalErrorsPerMinute() {
        Map<Instant, Integer> totalErrorsPerMinute = new HashMap<>();

        for (Map<String, Map<Instant, Integer>> errorCodeMap : errorCount.values()) {
            for (Map<Instant, Integer> minuteMap : errorCodeMap.values()) {
                for (Map.Entry<Instant, Integer> entry : minuteMap.entrySet()) {
                    totalErrorsPerMinute.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        return totalErrorsPerMinute;
    }
}

