package com.nirmala.logsense.repository;
import com.nirmala.logsense.entity.LogAnalysisRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LogAnalysisRunRepository extends JpaRepository<LogAnalysisRun, Long> {
    List<LogAnalysisRun> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
