package com.nirmala.logsense.service;

import com.nirmala.logsense.aggregator.Aggregator;
import com.nirmala.logsense.config.AnomalyDetectionConfig;
import com.nirmala.logsense.correlator.IncidentCorrelator;
import com.nirmala.logsense.detector.AnomalyDetector;
import com.nirmala.logsense.dto.LogAnalysisResponseDTO;
import com.nirmala.logsense.explainer.IncidentExplainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;

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

    @BeforeEach
    void setUp() {
        service = new LogAnalysisService(
                context, detector, correlator, explainer, config,logPersistenceService
        );

    }

    @Test
    void shouldReturnFailedResponseWhenFileIsEmpty() {
        // Arrange — empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.log", "text/plain", new byte[0]
        );

        // Act
        LogAnalysisResponseDTO result = service.runAnalysis(1L,emptyFile);

        // Assert
        assertEquals("failed", result.getStatus());
        assertEquals("Uploaded log file is empty", result.getMessage());
    }

    @Test
    void shouldReturnFailedResponseWhenFileIsNull() {
        // Act
        LogAnalysisResponseDTO result = service.runAnalysis(1L,null);

        // Assert
        assertEquals("failed", result.getStatus());
    }

//    @Test
//    void shouldReturnSuccessResponseForValidFile() throws Exception {
//        when(context.getBean(Aggregator.class)).thenReturn(aggregator);
//        when(detector.detect(any(), any())).thenReturn(new HashMap<>());
//        when(correlator.group(any())).thenReturn(new HashMap<>());
//
//        String logContent = "your correct log format here";
//        MockMultipartFile validFile = new MockMultipartFile(
//                "file", "test.log", "text/plain", logContent.getBytes()
//        );
//
//        LogAnalysisResponseDTO result = service.runAnalysis(validFile);
//
//        assertEquals("success", result.getStatus());
//        assertEquals("Analysis completed successfully", result.getMessage());
//    }
}