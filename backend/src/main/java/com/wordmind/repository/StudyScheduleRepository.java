package com.wordmind.repository;

import com.wordmind.entity.StudySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyScheduleRepository extends JpaRepository<StudySchedule, Long> {

    List<StudySchedule> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<StudySchedule> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, StudySchedule.ScheduleStatus status);

    @Query("SELECT ss FROM StudySchedule ss WHERE ss.userId = :userId AND ss.status = 'ACTIVE' " +
           "AND ss.startDate <= :today AND ss.endDate >= :today ORDER BY ss.createdAt DESC")
    List<StudySchedule> findActiveSchedulesForDate(@Param("userId") Long userId, @Param("today") LocalDate today);

    Optional<StudySchedule> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(DISTINCT sp.date) FROM ScheduleProgress sp WHERE sp.scheduleId = :scheduleId " +
           "AND sp.completedWordIds IS NOT NULL AND sp.completedWordIds != ''")
    Long countCompletedDays(@Param("scheduleId") Long scheduleId);
}
