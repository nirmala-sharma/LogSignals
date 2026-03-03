package com.nirmala.logsense.model;

import java.time.Instant;

public class Incident {

        private final Instant start;
        private Instant end;

        public Incident(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        public Instant getStart() { return start; }
        public Instant getEnd() { return end; }
        public void setEnd(Instant end) { this.end = end; }
    }
