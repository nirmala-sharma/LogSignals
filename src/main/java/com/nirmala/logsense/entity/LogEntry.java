package com.nirmala.logsense.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "logs")
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "analysis_run_id")
    private Long analysisRunId;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "service_name")
    private String serviceName;

    private String hostname;

    @Column(name = "error_code")
    private String errorCode;

    private String level;

    private String message;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "raw_line")
    private String rawLine;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
