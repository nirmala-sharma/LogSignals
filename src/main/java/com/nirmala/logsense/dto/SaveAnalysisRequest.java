package com.nirmala.logsense.dto;

import com.nirmala.logsense.model.LogModel;
import lombok.Data;

import java.util.List;

@Data
public class SaveAnalysisRequest {
    private List<LogModel> parsedLogs;
    private LogAnalysisResponseDTO response;
}
