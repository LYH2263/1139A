package com.wordmind.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "schedule_progress")
@Data
public class ScheduleProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "planned_word_ids", columnDefinition = "TEXT", nullable = false)
    private String plannedWordIds;

    @Column(name = "completed_word_ids", columnDefinition = "TEXT")
    private String completedWordIds;
}
