package com.nirmala.logsense.repository;

import com.nirmala.logsense.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByOwnerUserId(Long ownerUserId);
}
