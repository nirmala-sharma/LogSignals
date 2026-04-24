package com.nirmala.logsense.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
@Data
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_id")
    private Long appId;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    private String name;

    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}

