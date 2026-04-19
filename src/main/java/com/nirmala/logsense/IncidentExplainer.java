package com.nirmala.logsense;

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
            Map<String, Map<String, Map<Instant, List<LogModel>>>> errorLogsByServiceAndErrorCode) {

        int totalErrors = 0;

        Map<String, Map<Instant, List<LogModel>>> errorCodeMap =
                errorLogsByServiceAndErrorCode.get(service);

        if (errorCodeMap == null) {
            String explanation = buildExplanation(service, errorCode, incident, totalErrors);
            incident.setExplanation(explanation);

            // BEFORE: System.out.println(explanation)
            // AFTER: log.info(...) — structured, controllable logging
            log.info(explanation);
            log.info("----------------------------------");
            return;
        }

        Map<Instant, List<LogModel>> logsByMinute = errorCodeMap.get(errorCode);

        if (logsByMinute == null) {
            String explanation = buildExplanation(service, errorCode, incident, totalErrors);
            incident.setExplanation(explanation);

            log.info(explanation);
            log.info("----------------------------------");
            return;
        }

        Instant t = incident.getStart();

        while (!t.isAfter(incident.getEnd())) {
            List<LogModel> logs = logsByMinute.get(t);
            if (logs != null) {
                totalErrors += logs.size();
            }
            t = t.plusSeconds(60);
        }

        String explanation = buildExplanation(service, errorCode, incident, totalErrors);
        incident.setExplanation(explanation);

        log.info(explanation);
        log.info("----------------------------------");
    }

    private String buildExplanation(String service, String errorCode, Incident incident, int totalErrors) {
        return service + " had " + totalErrors + " " + errorCode +
                " errors at " + incident.getStart() + ".";
    }

    private String findTop(Map<String, Integer> counts) {
        String bestKey = "N/A";
        int bestVal = -1;

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestVal) {
                bestVal = entry.getValue();
                bestKey = entry.getKey();
            }
        }

        return bestKey;
    }
}