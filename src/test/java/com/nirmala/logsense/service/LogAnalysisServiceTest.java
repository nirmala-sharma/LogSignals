package com.nirmala.logsense.service;

import com.nirmala.logsense.aggregator.Aggregator;
import com.nirmala.logsense.config.AnomalyDetectionConfig;
import com.nirmala.logsense.correlator.IncidentCorrelator;
import com.nirmala.logsense.detector.AnomalyDetector;
import com.nirmala.logsense.exception.EmptyLogFileException;
import com.nirmala.logsense.explainer.IncidentExplainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LogAnalysisServiceTest {

    // @Mock creates a fake version of each dependency
    @Mock private ApplicationContext context;
    @Mock private AnomalyDetector detector;
    @Mock private IncidentCorrelator correlator;
    @Mock private IncidentExplainer explainer;
    @Mock private AnomalyDetectionConfig config;
    @Mock private Aggregator aggregator;
    @Mock private  LogPersistenceService logPersistenceService;
    private LogAnalysisService service;
    @Mock private AlertNotificationService alertNotificationService;

    @BeforeEach
    void setUp() {
        service = new LogAnalysisService(
                context, detector, correlator, explainer, config,logPersistenceService,alertNotificationService
        );

    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.log", "text/plain", new byte[0]
        );

        EmptyLogFileException ex = assertThrows(EmptyLogFileException.class,
                () -> service.runAnalysis(1L, emptyFile));
        assertEquals("Uploaded log file is empty", ex.getMessage());
    }

    @Test
    void shouldRejectNullFile() {
        assertThrows(EmptyLogFileException.class, () -> service.runAnalysis(1L, null));
    }
}
