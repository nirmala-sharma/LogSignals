package com.nirmala.logsense.config;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Data
@NoArgsConstructor
@Configuration  // tells Spring this is a config class
public class AnomalyDetectionConfig {
    @Value("${logsense.detection.windowSize}")
    private int windowSize;

    @Value("${logsense.detection.minimumStandardDeviation}")
    private double minimumStandardDeviation;

    @Value("${logsense.detection.minimumSamples}")
    private int minimumSamples;

    @Value("${logsense.detection.threshold}")
    private double threshold;
}
