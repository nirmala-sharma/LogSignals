package com.nirmala.logsense.correlator;

import com.nirmala.logsense.model.Incident;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class IncidentCorrelator {

    public Map<String, Map<String, List<Incident>>> group(
            Map<String, Map<String, List<Instant>>> anomaliesByServiceAndErrorCode) {

        Map<String, Map<String, List<Incident>>> incidentsByServiceAndErrorCode = new HashMap<>();

        if (anomaliesByServiceAndErrorCode == null || anomaliesByServiceAndErrorCode.isEmpty()) {
            return incidentsByServiceAndErrorCode;
        }

        for (Map.Entry<String, Map<String, List<Instant>>> serviceEntry
                : anomaliesByServiceAndErrorCode.entrySet()) {

            String service = serviceEntry.getKey();
            Map<String, List<Instant>> errorCodeMap = serviceEntry.getValue();

            for (Map.Entry<String, List<Instant>> errorEntry : errorCodeMap.entrySet()) {
                String errorCode = errorEntry.getKey();
                List<Instant> anomalyMinutes = new ArrayList<>(errorEntry.getValue());

                List<Incident> incidents = groupSingleStream(anomalyMinutes);

                if (!incidents.isEmpty()) {
                    incidentsByServiceAndErrorCode
                            .computeIfAbsent(service, s -> new HashMap<>())
                            .put(errorCode, incidents);
                }
            }
        }

        return incidentsByServiceAndErrorCode;
    }

    private List<Incident> groupSingleStream(List<Instant> anomalyMinutes) {
        List<Incident> incidents = new ArrayList<>();

        if (anomalyMinutes == null || anomalyMinutes.isEmpty()) {
            return incidents;
        }

        Collections.sort(anomalyMinutes);

        Incident current = new Incident(anomalyMinutes.get(0), anomalyMinutes.get(0));

        for (int i = 1; i < anomalyMinutes.size(); i++) {
            Instant minute = anomalyMinutes.get(i);
            Instant expectedNext = current.getEnd().plusSeconds(60);

            if (minute.equals(expectedNext)) {
                current.setEnd(minute);
            } else {
                incidents.add(current);
                current = new Incident(minute, minute);
            }
        }

        incidents.add(current);
        return incidents;
    }
}


