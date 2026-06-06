package com.wordmind.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "word_relations")
@Data
public class WordRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "source_word_id", nullable = false)
    private Long sourceWordId;
    
    @Column(name = "target_word_id", nullable = false)
    private Long targetWordId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 20)
    private RelationType relationType;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RelationStatus status = RelationStatus.APPROVED;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum RelationType {
        SYNONYM,
        ANTONYM,
        TOPIC,
        ROOT,
        PREFIX,
        SUFFIX,
        SCENE
    }
    
    public enum RelationStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
