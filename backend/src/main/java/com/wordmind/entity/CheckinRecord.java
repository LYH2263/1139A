package com.wordmind.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkin_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "checkin_date"})
})
@Data
public class CheckinRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkin_source", length = 20)
    private CheckinSource checkinSource;

    @Column(name = "achievement_unlocked", length = 50)
    private String achievementUnlocked;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum CheckinSource {
        REVIEW,
        QUIZ,
        MANUAL
    }
}
