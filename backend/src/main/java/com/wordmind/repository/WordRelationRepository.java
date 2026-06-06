package com.wordmind.repository;

import com.wordmind.entity.WordRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordRelationRepository extends JpaRepository<WordRelation, Long> {
    
    @Query("SELECT wr FROM WordRelation wr WHERE (wr.sourceWordId = :wordId OR wr.targetWordId = :wordId) AND wr.status = 'APPROVED'")
    List<WordRelation> findByWordId(@Param("wordId") Long wordId);
    
    @Query("SELECT wr FROM WordRelation wr WHERE " +
           "(wr.sourceWordId IN :wordIds OR wr.targetWordId IN :wordIds) AND " +
           "wr.sourceWordId IN :wordIds AND wr.targetWordId IN :wordIds AND wr.status = 'APPROVED'")
    List<WordRelation> findRelationsBetweenWords(@Param("wordIds") List<Long> wordIds);
    
    boolean existsBySourceWordIdAndTargetWordIdAndRelationType(Long sourceWordId, Long targetWordId, WordRelation.RelationType relationType);
    
    @Query("SELECT wr FROM WordRelation wr WHERE wr.status = 'PENDING' ORDER BY wr.createdAt DESC")
    List<WordRelation> findAllPending();
    
    @Query("SELECT wr FROM WordRelation wr WHERE (wr.sourceWordId = :wordId OR wr.targetWordId = :wordId)")
    List<WordRelation> findAllByWordId(@Param("wordId") Long wordId);
}
