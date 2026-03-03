package com.nirmala.logsense;
import com.nirmala.logsense.model.Incident;
import com.nirmala.logsense.model.LogModel;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncidentExplainer {

    public void explainIncident(Incident incident,
                                Map<Instant, List<LogModel>> errorLogsByMinute) {

        Map<String, Integer> serviceCount = new HashMap<>();
        Map<String, Integer> errorCodeCount = new HashMap<>();
        int totalErrors = 0;

        Instant t = incident.getStart();
        while (!t.isAfter(incident.getEnd())) {

            List<LogModel> logs = errorLogsByMinute.get(t);
            if (logs != null) {
                for (LogModel log : logs) {
                    totalErrors++;

                    // service count
                    String svc = log.getService();
                    serviceCount.put(svc, serviceCount.getOrDefault(svc, 0) + 1);

                    // error code count
                    String code = log.getErrorCode();
                    if (code == null) code = "UNKNOWN";
                    errorCodeCount.put(code, errorCodeCount.getOrDefault(code, 0) + 1);
                }
            }

            t = t.plusSeconds(60);
        }

        String topService = findTop(serviceCount);
        String topErrorCode = findTop(errorCodeCount);

        System.out.println("INCIDENT: " + incident.getStart() + " -> " + incident.getEnd());
        System.out.println("Total Errors: " + totalErrors);
        System.out.println("Top Service: " + topService);
        System.out.println("Top Error Code: " + topErrorCode);
        System.out.println("----------------------------------");
    }

    private String findTop(Map<String, Integer> counts) {
        String bestKey = "N/A";
        int bestVal = -1;

        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestVal) {
                bestVal = e.getValue();
                bestKey = e.getKey();
            }
        }
        return bestKey;
    }
}
