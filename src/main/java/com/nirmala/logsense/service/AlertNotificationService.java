package com.nirmala.logsense.service;

import com.nirmala.logsense.entity.User;
import com.nirmala.logsense.entity.Application;
import com.nirmala.logsense.repository.AppUserRepository;
import com.nirmala.logsense.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AlertNotificationService {

    private final ApplicationRepository applicationRepository;
    private final AppUserRepository appUserRepository;
    private final EmailAlertService emailAlertService;

    public AlertNotificationService(
            ApplicationRepository applicationRepository,
            AppUserRepository appUserRepository,
            EmailAlertService emailAlertService
    ) {
        this.applicationRepository = applicationRepository;
        this.appUserRepository = appUserRepository;
        this.emailAlertService = emailAlertService;
    }

    public void sendAlertsIfNeeded(
            Long applicationId,
            Map<String, Map<String, List<Instant>>> anomalies
    ) {
        if (anomalies == null || anomalies.isEmpty()) {
            return;
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        User owner = appUserRepository.findById(application.getOwnerUserId())
                .orElseThrow(() -> new RuntimeException("Application owner not found"));

        for (Map.Entry<String, Map<String, List<Instant>>> serviceEntry : anomalies.entrySet()) {
            String serviceName = serviceEntry.getKey();
            Map<String, List<Instant>> errorMap = serviceEntry.getValue();

            for (Map.Entry<String, List<Instant>> errorEntry : errorMap.entrySet()) {
                String errorCode = errorEntry.getKey();
                List<Instant> timestamps = errorEntry.getValue();

                String subject = "[LogSignals] Anomaly detected in " + application.getName();

                String body = """
                        An anomaly was detected in LogSignals.

                        Application: %s
                        Service: %s
                        Error Code: %s
                        Occurrences: %d
                        First Detected At: %s

                        Please review the recent logs and anomalies in the system.
                        """.formatted(
                        application.getName(),
                        serviceName,
                        errorCode,
                        timestamps.size(),
                        timestamps.isEmpty() ? "N/A" : timestamps.get(0)
                );

                emailAlertService.sendAlert(owner.getEmail(), subject, body);
            }
        }
    }
}
