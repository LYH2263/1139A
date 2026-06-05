package com.wordmind.repository;

import com.wordmind.entity.ScheduleProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleProgressRepository extends JpaRepository<ScheduleProgress, Long> {

    List<ScheduleProgress> findByScheduleIdOrderByDateAsc(Long scheduleId);

    Optional<ScheduleProgress> findByScheduleIdAndDate(Long scheduleId, LocalDate date);

    @Query("SELECT sp FROM ScheduleProgress sp WHERE sp.scheduleId = :scheduleId " +
           "AND sp.date <= :today ORDER BY sp.date ASC")
    List<ScheduleProgress> findProgressUpToDate(@Param("scheduleId") Long scheduleId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(DISTINCT sp.date) FROM ScheduleProgress sp WHERE sp.scheduleId = :scheduleId " +
           "AND sp.completedWordIds IS NOT NULL AND sp.completedWordIds != ''")
    Long countCompletedDays(@Param("scheduleId") Long scheduleId);

    void deleteByScheduleId(Long scheduleId);
}
