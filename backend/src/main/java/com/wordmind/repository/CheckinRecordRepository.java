package com.wordmind.repository;

import com.wordmind.entity.CheckinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinRecordRepository extends JpaRepository<CheckinRecord, Long> {

    Optional<CheckinRecord> findByUserIdAndCheckinDate(Long userId, LocalDate checkinDate);

    @Query("SELECT cr FROM CheckinRecord cr WHERE cr.userId = :userId " +
           "AND cr.checkinDate >= :startDate AND cr.checkinDate <= :endDate " +
           "ORDER BY cr.checkinDate ASC")
    List<CheckinRecord> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(cr) FROM CheckinRecord cr WHERE cr.userId = :userId")
    Long countTotalCheckins(@Param("userId") Long userId);

    @Query("SELECT cr.checkinDate FROM CheckinRecord cr WHERE cr.userId = :userId " +
           "ORDER BY cr.checkinDate DESC")
    List<LocalDate> findAllCheckinDatesDesc(@Param("userId") Long userId);
}
