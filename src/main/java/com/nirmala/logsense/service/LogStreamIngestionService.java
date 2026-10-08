package com.nirmala.logsense.service;

import com.nirmala.logsense.dto.LiveIngestResponseDTO;
import com.nirmala.logsense.dto.StreamIngestResponseDTO;
import com.nirmala.logsense.exception.LogAnalysisException;
import com.nirmala.logsense.model.LogModel;
import com.nirmala.logsense.parser.LogParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class LogStreamIngestionService {

    private final LiveLogIngestionService liveLogIngestionService;

    public LogStreamIngestionService(LiveLogIngestionService liveLogIngestionService){
        this.liveLogIngestionService = liveLogIngestionService;

    }
    public StreamIngestResponseDTO ingestStream(Long applicationId, InputStream body){
        int totalLines = 0;
        int ingestedLines = 0;
        int invalidLines = 0;
        int anomalousLines = 0;

        try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(body,
                        StandardCharsets.UTF_8)
        )){
            String line;
            // readline() waits until the next line arrives; it returns null when the clients ends the stream
            while ((line = reader.readLine()) != null) {

                if(line.isBlank()){
                    continue;
                }
                totalLines++;
                LogModel logModel;
                try{
                    logModel  = LogParser.parse(line);

                }
                catch(Exception e){
                    invalidLines++;
                    log.warn("Invalid log line skipped in stream. applicationId={},line={}",applicationId,line);
                    continue;
                }
                LiveIngestResponseDTO result = liveLogIngestionService.ingest(applicationId,logModel);
                ingestedLines++;
                if(result.isAnomalyDetected()){
                    anomalousLines++;
                }
            }
        } catch (IOException e) {
            throw new LogAnalysisException("Failed to read log stream",e);
        }
        log.info("Log stream finished. applicationId={},totalLines={},ingested={},invalid={},anomalous={}",applicationId,totalLines,ingestedLines,invalidLines,anomalousLines);

        StreamIngestResponseDTO response = new StreamIngestResponseDTO();
        response.setStatus("success");
        response.setMessage("Stream processed");
        response.setTotalLines(totalLines);
        response.setIngestedLines(ingestedLines);
        response.setInvalidLines(invalidLines);
        response.setAnomalousLines(anomalousLines);
        return response;
    }




}
