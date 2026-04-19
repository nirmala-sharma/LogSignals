package com.nirmala.logsense.model;

import java.time.Instant;
import java.util.Objects;

public class AggregationKey {

    private final String service;
    private final String errorCode;
    private final Instant minuteBucket;

    public AggregationKey(String service, String errorCode, Instant minuteBucket) {
        this.service = service;
        this.errorCode = errorCode;
        this.minuteBucket = minuteBucket;
    }

    public String getService() { return service; }
    public String getErrorCode() { return errorCode; }
    public Instant getMinuteBucket() { return minuteBucket; }

    // IMPORTANT — needed for HashMap to work correctly
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AggregationKey)) return false;
        AggregationKey that = (AggregationKey) o;
        return Objects.equals(service, that.service) &&
                Objects.equals(errorCode, that.errorCode) &&
                Objects.equals(minuteBucket, that.minuteBucket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, errorCode, minuteBucket);
    }

    @Override
    public String toString() {
        return "AggregationKey{" +
                "service='" + service + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", minuteBucket=" + minuteBucket +
                '}';
    }
}
