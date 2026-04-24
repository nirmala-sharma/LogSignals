package com.nirmala.logsense.service;

import com.nirmala.logsense.dto.LogAnalysisResponseDTO;
import com.nirmala.logsense.entity.Anomaly;
import com.nirmala.logsense.entity.LogAnalysisRun;
import com.nirmala.logsense.entity.LogEntry;
import com.nirmala.logsense.model.LogModel;
import com.nirmala.logsense.repository.AnomalyRepository;
import com.nirmala.logsense.repository.LogAnalysisRunRepository;
import com.nirmala.logsense.repository.LogEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LogPersistenceService {

    private final LogAnalysisRunRepository logAnalysisRunRepository;
    private final LogEntryRepository logEntryRepository;
    private final AnomalyRepository anomalyRepository;

    public LogPersistenceService(
            LogAnalysisRunRepository logAnalysisRunRepository,
            LogEntryRepository logEntryRepository,
            AnomalyRepository anomalyRepository
    ) {
        this.logAnalysisRunRepository = logAnalysisRunRepository;
        this.logEntryRepository = logEntryRepository;
        this.anomalyRepository = anomalyRepository;
    }

    @Transactional
    public LogAnalysisRun saveAnalysisResult(
            Long applicationId,
            List<LogModel> parsedLogs,
            LogAnalysisResponseDTO response
    ) {
        LogAnalysisRun run = new LogAnalysisRun();
        run.setApplicationId(applicationId);
        run.setStatus(response.getStatus());
        run.setMessage(response.getMessage());
        run.setTotalLines(response.getTotalLines());
        run.setInvalidLines(response.getInvalidLines());

        LogAnalysisRun savedRun = logAnalysisRunRepository.save(run);

        saveLogs(applicationId, savedRun.getLogRunId(), parsedLogs);
        saveAnomalies(applicationId, savedRun.getLogRunId(), response.getAnomalies());

        return savedRun;
    }

    private void saveLogs(Long applicationId, Long analysisRunId, List<LogModel> parsedLogs) {
        List<LogEntry> entries = new ArrayList<>();

        for (LogModel logModel : parsedLogs) {
            LogEntry entry = new LogEntry();
            entry.setApplicationId(applicationId);
            entry.setAnalysisRunId(analysisRunId);
            entry.setServiceName(logModel.getService());
            entry.setErrorCode(logModel.getErrorCode());
            entry.setLevel(logModel.getLevel());
            entry.setMessage(logModel.getMessage());
            entry.setOccurredAt(logModel.getTimestamp());

            entries.add(entry);
        }

        logEntryRepository.saveAll(entries);
    }

    private void saveAnomalies(
            Long applicationId,
            Long analysisRunId,
            Map<String, Map<String, List<Instant>>> anomalies
    ) {
        if (anomalies == null || anomalies.isEmpty()) {
            return;
        }

        List<Anomaly> anomalyEntities = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<Instant>>> serviceEntry : anomalies.entrySet()) {
            String serviceName = serviceEntry.getKey();
            Map<String, List<Instant>> errorCodeMap = serviceEntry.getValue();

            for (Map.Entry<String, List<Instant>> errorEntry : errorCodeMap.entrySet()) {
                String errorCode = errorEntry.getKey();
                List<Instant> timestamps = errorEntry.getValue();

                for (Instant occurredAt : timestamps) {
                    Anomaly anomaly = new Anomaly();
                    anomaly.setApplicationId(applicationId);
                    anomaly.setAnalysisRunId(analysisRunId);
                    anomaly.setServiceName(serviceName);
                    anomaly.setErrorCode(errorCode);
                    anomaly.setOccurredAt(occurredAt);

                    anomalyEntities.add(anomaly);
                }
            }
        }

        anomalyRepository.saveAll(anomalyEntities);
    }
    @Transactional
    public LogAnalysisRun saveAnalysisSummaryAndAnomalies(
            Long applicationId,
            LogAnalysisResponseDTO response
    ) {
        LogAnalysisRun run = new LogAnalysisRun();
        run.setApplicationId(applicationId);
        run.setStatus(response.getStatus());
        run.setMessage(response.getMessage());
        run.setTotalLines(response.getTotalLines());
        run.setInvalidLines(response.getInvalidLines());

        LogAnalysisRun savedRun = logAnalysisRunRepository.save(run);

        saveAnomalies(
                applicationId,
                savedRun.getLogRunId(),
                response.getAnomalies()
        );

        return savedRun;
    }
    @Transactional
    public LogAnalysisRun saveLiveIngestResult(
            Long applicationId,
            LogModel logModel,
            Map<String, Map<String, List<Instant>>> anomalies
    ) {
        LogAnalysisRun run = new LogAnalysisRun();
        run.setApplicationId(applicationId);
        run.setStatus("SUCCESS");
        run.setMessage("Live log ingested");
        run.setTotalLines(1);
        run.setInvalidLines(0);

        LogAnalysisRun savedRun = logAnalysisRunRepository.save(run);

        LogEntry entry = new LogEntry();
        entry.setApplicationId(applicationId);
        entry.setAnalysisRunId(savedRun.getLogRunId());
        entry.setServiceName(logModel.getService());
        entry.setErrorCode(logModel.getErrorCode());
        entry.setLevel(logModel.getLevel());
        entry.setMessage(logModel.getMessage());
        entry.setOccurredAt(logModel.getTimestamp());

        logEntryRepository.save(entry);

        saveAnomalies(
                applicationId,
                savedRun.getLogRunId(),
                anomalies
        );

        return savedRun;
    }
}
