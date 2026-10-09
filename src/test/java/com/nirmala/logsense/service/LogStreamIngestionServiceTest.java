package com.nirmala.logsense.service;

import com.nirmala.logsense.dto.LiveIngestResponseDTO;
import com.nirmala.logsense.dto.StreamIngestResponseDTO;
import com.nirmala.logsense.model.LogModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class LogStreamIngestionServiceTest {
private static final long APP_ID = 1L;

private static final String VALID_LINE =
        "{\"timestamp\":\"2026-02-04T10:00:10Z\",\"level\":\"ERROR\",\"service\":\"DBService\","
                + "\"errorCode\":\"DB_TIMEOUT\",\"message\":\"Database connection timeout\"}";

@Mock
private LiveLogIngestionService liveLogIngestionService;

private LogStreamIngestionService service;

@BeforeEach
    void setUp() {
    service = new LogStreamIngestionService(liveLogIngestionService);
}

/* Builds a fake "stream" from lines, joined with newlines, exactly like an NDJSON request body */
private static InputStream stream(String... lines){
    return new ByteArrayInputStream(String.join("\n",lines).getBytes(StandardCharsets.UTF_8));
}

private static LiveIngestResponseDTO pipelineResult(boolean anomalyDetected)
{
    LiveIngestResponseDTO result = new LiveIngestResponseDTO();
    result.setAnomalyDetected(anomalyDetected);
    return result;
}

    @Test
    void shouldIngestValidLinesAndSkipInvalidAndBlankLines() {
        when(liveLogIngestionService.ingest(eq(APP_ID), any(LogModel.class)))
                .thenReturn(pipelineResult(false));

        StreamIngestResponseDTO response =
                service.ingestStream(APP_ID, stream(VALID_LINE, "this is not json", "", VALID_LINE));

        assertEquals("success", response.getStatus());
        assertEquals(3, response.getTotalLines());      // blank line is not counted
        assertEquals(2, response.getIngestedLines());
        assertEquals(1, response.getInvalidLines());
        assertEquals(0, response.getAnomalousLines());
        verify(liveLogIngestionService, times(2)).ingest(eq(APP_ID), any(LogModel.class));
    }

    @Test
    void shouldCountLinesThatLandInAnAnomaly() {
        when(liveLogIngestionService.ingest(eq(APP_ID), any(LogModel.class)))
                .thenReturn(pipelineResult(false), pipelineResult(true), pipelineResult(true));

        StreamIngestResponseDTO response =
                service.ingestStream(APP_ID, stream(VALID_LINE, VALID_LINE, VALID_LINE));

        assertEquals(3, response.getIngestedLines());
        assertEquals(2, response.getAnomalousLines());
    }

    @Test
    void shouldHandleEmptyStream() {
        StreamIngestResponseDTO response = service.ingestStream(APP_ID, stream());

        assertEquals(0, response.getTotalLines());
        assertEquals(0, response.getIngestedLines());
        verifyNoInteractions(liveLogIngestionService);
    }
}
