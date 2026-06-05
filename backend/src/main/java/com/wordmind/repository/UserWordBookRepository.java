package com.wordmind.repository;

import com.wordmind.entity.UserWordBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWordBookRepository extends JpaRepository<UserWordBook, Long> {

    List<UserWordBook> findByUserId(Long userId);

    Optional<UserWordBook> findByUserIdAndWordBookId(Long userId, Long wordBookId);

    boolean existsByUserIdAndWordBookId(Long userId, Long wordBookId);

    @Transactional
    void deleteByUserIdAndWordBookId(Long userId, Long wordBookId);

    @Query("SELECT COUNT(DISTINCT rr.wordId) FROM ReviewRecord rr " +
           "WHERE rr.userId = :userId AND rr.result = 'KNOWN' " +
           "AND rr.wordId IN (SELECT wbw.wordId FROM WordBookWord wbw WHERE wbw.wordBookId = :wordBookId)")
    Long countMasteredWordsByUserIdAndWordBookId(@Param("userId") Long userId, @Param("wordBookId") Long wordBookId);
}
