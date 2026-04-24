package com.nirmala.logsense.repository;

import com.nirmala.logsense.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByApplicationIdOrderByOccurredAtDesc(Long applicationId);
    List<LogEntry> findByAnalysisRunId(Long analysisRunId);
}
