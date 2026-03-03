package com.nirmala.logsense;

import com.nirmala.logsense.model.Incident;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IncidentCorrelator {
    public List<Incident> group(List<Instant> anomalyMinutes) {
        List<Incident> incidents = new ArrayList<>();
        if (anomalyMinutes == null || anomalyMinutes.isEmpty()) return incidents;

        Collections.sort(anomalyMinutes);

        // start first incident
        Incident current = new Incident(anomalyMinutes.get(0), anomalyMinutes.get(0));

        // start first incident
        for (int i = 1; i < anomalyMinutes.size(); i++) {
            Instant minute = anomalyMinutes.get(i);
            // check if this minute is exactly 1 minute after previous
            Instant expectedNext = current.getEnd().plusSeconds(60);

            // same incident → extend end time
            if (minute.equals(expectedNext)) {
                current.setEnd(minute);
            } else {
                incidents.add(current);
                current = new Incident(minute, minute); // new incident
            }
        }
        // add last incident
        incidents.add(current);
        return incidents;
    }
}
