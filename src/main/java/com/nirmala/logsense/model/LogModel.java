package com.nirmala.logsense.model;

import java.time.Instant;

public class LogModel {

    private Instant timestamp;
    private String level;
    private String service;
    private String errorCode;
    private String message;

    public LogModel(Instant timestamp, String level, String service,
                    String errorCode, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.service = service;
        this.errorCode = errorCode;
        this.message = message;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getLevel() { return level; }
    public String getService() { return service; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", level='" + level + '\'' +
                ", service='" + service + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
