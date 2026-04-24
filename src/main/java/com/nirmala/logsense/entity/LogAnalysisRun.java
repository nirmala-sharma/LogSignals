package com.nirmala.logsense.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "log_analysis_runs")
public class LogAnalysisRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_run_id")
    private Long logRunId;

    @Column(name = "application_id")
    private Long applicationId;

    private String status;

    private String message;

    @Column(name = "total_lines")
    private Integer totalLines;

    @Column(name = "invalid_lines")
    private Integer invalidLines;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
