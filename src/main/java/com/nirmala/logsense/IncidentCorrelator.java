package com.nirmala.logsense;

import com.nirmala.logsense.model.Incident;

import java.time.Instant;
import java.util.*;

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














//public class IncidentCorrelator {
//    public List<Incident> group(List<Instant> anomalyMinutes) {
//        List<Incident> incidents = new ArrayList<>();
//        if (anomalyMinutes == null || anomalyMinutes.isEmpty()) return incidents;
//
//        Collections.sort(anomalyMinutes);
//
//        // start first incident
//        Incident current = new Incident(anomalyMinutes.get(0), anomalyMinutes.get(0));
//
//        // start first incident
//        for (int i = 1; i < anomalyMinutes.size(); i++) {
//            Instant minute = anomalyMinutes.get(i);
//            // check if this minute is exactly 1 minute after previous
//            Instant expectedNext = current.getEnd().plusSeconds(60);
//
//            // same incident → extend end time
//            if (minute.equals(expectedNext)) {
//                current.setEnd(minute);
//            } else {
//                incidents.add(current);
//                current = new Incident(minute, minute); // new incident
//            }
//        }
//        // add last incident
//        incidents.add(current);
//        return incidents;
//    }
//}
