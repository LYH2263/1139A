package com.wordmind.repository;

import com.wordmind.entity.SharedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SharedReportRepository extends JpaRepository<SharedReport, Long> {

    @Query("SELECT sr FROM SharedReport sr WHERE sr.token = :token AND sr.expiresAt > :now")
    Optional<SharedReport> findValidByToken(@Param("token") String token, @Param("now") LocalDateTime now);
}
