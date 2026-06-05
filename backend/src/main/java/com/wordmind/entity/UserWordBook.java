package com.wordmind.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_word_books", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "word_book_id"})
})
@Data
public class UserWordBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_book_id", nullable = false)
    private Long wordBookId;

    @Column(name = "mastered_count")
    private Integer masteredCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
