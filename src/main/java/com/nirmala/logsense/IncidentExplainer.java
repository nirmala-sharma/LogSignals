package com.nirmala.logsense;
import com.nirmala.logsense.model.Incident;
import com.nirmala.logsense.model.LogModel;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
            System.out.println("INCIDENT: " + incident.getStart() + " -> " + incident.getEnd());
            System.out.println("Service: " + service);
            System.out.println("Error Code: " + errorCode);
            System.out.println("Total Errors: 0");
            System.out.println("----------------------------------");
            return;
        }

        Map<Instant, List<LogModel>> logsByMinute = errorCodeMap.get(errorCode);

        if (logsByMinute == null) {
            System.out.println("INCIDENT: " + incident.getStart() + " -> " + incident.getEnd());
            System.out.println("Service: " + service);
            System.out.println("Error Code: " + errorCode);
            System.out.println("Total Errors: 0");
            System.out.println("----------------------------------");
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

        System.out.println("INCIDENT: " + incident.getStart() + " -> " + incident.getEnd());
        System.out.println("Service: " + service);
        System.out.println("Error Code: " + errorCode);
        System.out.println("Total Errors: " + totalErrors);
        System.out.println("----------------------------------");
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
