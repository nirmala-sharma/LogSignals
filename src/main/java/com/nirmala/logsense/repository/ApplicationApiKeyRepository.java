package com.nirmala.logsense.repository;

import com.nirmala.logsense.entity.ApplicationApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApplicationApiKeyRepository extends JpaRepository<ApplicationApiKey, Long> {
    Optional<ApplicationApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);
}
