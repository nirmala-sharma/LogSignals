package com.nirmala.logsense.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "application_api_keys")
public class ApplicationApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "key_hash")
    private String keyHash;

    @Column(name = "key_prefix")
    private String keyPrefix;

    private String name;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
