package com.wordmind.repository;

import com.wordmind.entity.WordBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WordBookRepository extends JpaRepository<WordBook, Long> {

    @Query("SELECT wb FROM WordBook wb WHERE " +
           "(:keyword IS NULL OR LOWER(wb.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(wb.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:difficulty IS NULL OR wb.difficultyLevel = :difficulty)")
    Page<WordBook> searchWordBooks(@Param("keyword") String keyword,
                                   @Param("difficulty") WordBook.DifficultyLevel difficulty,
                                   Pageable pageable);

    @Query("SELECT wb FROM WordBook wb JOIN UserWordBook uwb ON wb.id = uwb.wordBookId " +
           "WHERE uwb.userId = :userId")
    List<WordBook> findByUserId(@Param("userId") Long userId);

    boolean existsByName(String name);
}
