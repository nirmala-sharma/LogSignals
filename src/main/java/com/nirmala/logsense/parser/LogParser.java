package com.nirmala.logsense.parser;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirmala.logsense.model.LogModel;
import java.time.Instant;

public class LogParser {

        private static final ObjectMapper mapper = new ObjectMapper();
        public static LogModel parse(String logLine) throws Exception {

            JsonNode node = mapper.readTree(logLine);

            Instant timestamp = Instant.parse(node.get("timestamp").asText());
            String level = node.get("level").asText();
            String service = node.get("service").asText();
            String message = node.get("message").asText();

            String errorCode = node.has("errorCode")
                    ? node.get("errorCode").asText()
                    : null;

            return new LogModel(timestamp, level, service, errorCode, message);
        }
    }



