package com.nirmala.logsense.explainer;

import com.nirmala.logsense.model.AggregationKey;
import com.nirmala.logsense.model.Incident;
import com.nirmala.logsense.model.LogModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class IncidentExplainer {

    public void explainIncident(
            String service,
            String errorCode,
            Incident incident,
            Map<AggregationKey, List<LogModel>> errorLogs) {

        int totalErrors = 0;
        Instant t = incident.getStart();

        while (!t.isAfter(incident.getEnd())) {
            // build the key and do a direct lookup
            AggregationKey key = new AggregationKey(service, errorCode, t);
            List<LogModel> logs = errorLogs.get(key);

            if (logs != null) {
                totalErrors += logs.size();
            }
            t = t.plusSeconds(60);
        }
        String severity = determineSeverity(totalErrors);
        String explanation = buildExplanation(service, errorCode, incident, totalErrors, severity);
        incident.setExplanation(explanation);
        log.info(explanation);
        log.info("----------------------------------");
    }

    private String buildExplanation(String service, String errorCode,
                                    Incident incident, int totalErrors, String severity) {
        return severity + ": " + service + " had " + totalErrors + " " + errorCode +
                " errors at " + incident.getStart() + ".";
    }
    private String determineSeverity(int errorCount) {
        if (errorCount >= 5) {
            return "CRITICAL";
        }
        if (errorCount >= 3) {
            return "HIGH";
        }
        return "MEDIUM";
    }
}