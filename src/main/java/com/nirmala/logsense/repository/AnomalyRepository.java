package com.nirmala.logsense.repository;

import com.nirmala.logsense.entity.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {
    List<Anomaly> findByApplicationIdOrderByOccurredAtDesc(Long applicationId);
    List<Anomaly> findByAnalysisRunId(Long analysisRunId);
}