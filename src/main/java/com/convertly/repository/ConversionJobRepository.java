package com.convertly.repository;

import com.convertly.entity.ConversionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversionJobRepository extends JpaRepository<ConversionJob, String> {

    Optional<ConversionJob> findByStorageKey(String storageKey);

    List<ConversionJob> findByExpiresAtBefore(Instant timestamp);

    List<ConversionJob> findTop20ByOrderByCreatedAtDesc();
}

