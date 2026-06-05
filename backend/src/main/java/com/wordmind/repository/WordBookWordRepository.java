package com.wordmind.repository;

import com.wordmind.entity.WordBookWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface WordBookWordRepository extends JpaRepository<WordBookWord, Long> {

    List<WordBookWord> findByWordBookId(Long wordBookId);

    List<WordBookWord> findByWordId(Long wordId);

    @Transactional
    void deleteByWordBookIdAndWordId(Long wordBookId, Long wordId);

    @Transactional
    void deleteByWordBookId(Long wordBookId);

    boolean existsByWordBookIdAndWordId(Long wordBookId, Long wordId);

    @Query("SELECT w.id FROM Word w JOIN WordBookWord wbw ON w.id = wbw.wordId " +
           "WHERE wbw.wordBookId = :wordBookId")
    List<Long> findWordIdsByWordBookId(@Param("wordBookId") Long wordBookId);

    @Query("SELECT COUNT(wbw) FROM WordBookWord wbw WHERE wbw.wordBookId = :wordBookId")
    Long countByWordBookId(@Param("wordBookId") Long wordBookId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WordBookWord wbw WHERE wbw.wordBookId = :wordBookId AND wbw.wordId IN :wordIds")
    void deleteByWordBookIdAndWordIdIn(@Param("wordBookId") Long wordBookId, @Param("wordIds") List<Long> wordIds);
}
