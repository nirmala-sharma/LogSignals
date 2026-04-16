package com.nirmala.logsense.exception;

public class LogParseException extends RuntimeException {

    public LogParseException(String message) {
        super(message);
    }

    public LogParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
