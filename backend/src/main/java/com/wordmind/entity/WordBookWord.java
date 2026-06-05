package com.wordmind.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "word_book_words", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"word_book_id", "word_id"})
})
@Data
public class WordBookWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "word_book_id", nullable = false)
    private Long wordBookId;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
