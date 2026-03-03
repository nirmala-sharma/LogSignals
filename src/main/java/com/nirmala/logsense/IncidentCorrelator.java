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

        Incident current = new Incident(anomalyMinutes.get(0), anomalyMinutes.get(0));

        for (int i = 1; i < anomalyMinutes.size(); i++) {
            Instant minute = anomalyMinutes.get(i);
            Instant expectedNext = current.getEnd().plusSeconds(60);

            if (minute.equals(expectedNext)) {
                current.setEnd(minute); // extend same incident
            } else {
                incidents.add(current);
                current = new Incident(minute, minute); // new incident
            }
        }

        incidents.add(current);
        return incidents;
    }
}
