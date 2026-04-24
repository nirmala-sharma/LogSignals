package com.nirmala.logsense.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "anomalies")
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "anomaly_id")
    private Long anomalyId;

    @Column(name = "analysis_run_id")
    private Long analysisRunId;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
