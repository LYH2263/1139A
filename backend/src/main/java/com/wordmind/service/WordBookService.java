package com.wordmind.service;

import com.wordmind.dto.WordBookDTO;
import com.wordmind.entity.UserWordBook;
import com.wordmind.entity.Word;
import com.wordmind.entity.WordBook;
import com.wordmind.entity.WordBookWord;
import com.wordmind.repository.UserWordBookRepository;
import com.wordmind.repository.WordBookRepository;
import com.wordmind.repository.WordBookWordRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WordBookService {

    @Autowired
    private WordBookRepository wordBookRepository;

    @Autowired
    private WordBookWordRepository wordBookWordRepository;

    @Autowired
    private UserWordBookRepository userWordBookRepository;

    @Autowired
    private WordRepository wordRepository;

    public WordBookDTO.ListResponse getAllWordBooks(String keyword, String difficulty, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        WordBook.DifficultyLevel level = difficulty != null ? WordBook.DifficultyLevel.valueOf(difficulty.toUpperCase()) : null;
        Page<WordBook> wordBookPage = wordBookRepository.searchWordBooks(keyword, level, pageable);

        List<WordBookDTO.Response> list = wordBookPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return WordBookDTO.ListResponse.builder()
                .list(list)
                .total(wordBookPage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }

    public WordBookDTO.Response getWordBookById(Long id) {
        WordBook wordBook = wordBookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("词书不存在"));
        return convertToDTO(wordBook);
    }

    public List<Word> getWordBookWords(Long wordBookId) {
        if (!wordBookRepository.existsById(wordBookId)) {
            throw new RuntimeException("词书不存在");
        }
        List<Long> wordIds = wordBookWordRepository.findWordIdsByWordBookId(wordBookId);
        return wordRepository.findAllById(wordIds);
    }

    @Transactional
    public WordBookDTO.Response createWordBook(WordBookDTO.CreateRequest request) {
        if (wordBookRepository.existsByName(request.getName())) {
            throw new RuntimeException("词书名称已存在");
        }

        WordBook wordBook = new WordBook();
        wordBook.setName(request.getName());
        wordBook.setDescription(request.getDescription());
        wordBook.setCoverImage(request.getCoverImage());
        wordBook.setDifficultyLevel(WordBook.DifficultyLevel.valueOf(request.getDifficultyLevel().toUpperCase()));

        WordBook saved = wordBookRepository.save(wordBook);
        return convertToDTO(saved);
    }

    @Transactional
    public WordBookDTO.Response updateWordBook(Long id, WordBookDTO.UpdateRequest request) {
        WordBook wordBook = wordBookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("词书不存在"));

        if (request.getName() != null && !request.getName().equals(wordBook.getName())) {
            if (wordBookRepository.existsByName(request.getName())) {
                throw new RuntimeException("词书名称已存在");
            }
            wordBook.setName(request.getName());
        }
        if (request.getDescription() != null) wordBook.setDescription(request.getDescription());
        if (request.getCoverImage() != null) wordBook.setCoverImage(request.getCoverImage());
        if (request.getDifficultyLevel() != null) {
            wordBook.setDifficultyLevel(WordBook.DifficultyLevel.valueOf(request.getDifficultyLevel().toUpperCase()));
        }

        WordBook saved = wordBookRepository.save(wordBook);
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteWordBook(Long id) {
        WordBook wordBook = wordBookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("词书不存在"));
        wordBookWordRepository.deleteByWordBookId(id);
        wordBookRepository.delete(wordBook);
    }

    @Transactional
    public void addWordsToWordBook(Long wordBookId, List<Long> wordIds) {
        if (!wordBookRepository.existsById(wordBookId)) {
            throw new RuntimeException("词书不存在");
        }

        List<Word> words = wordRepository.findAllById(wordIds);
        if (words.size() != wordIds.size()) {
            throw new RuntimeException("部分单词不存在");
        }

        for (Long wordId : wordIds) {
            if (!wordBookWordRepository.existsByWordBookIdAndWordId(wordBookId, wordId)) {
                WordBookWord wordBookWord = new WordBookWord();
                wordBookWord.setWordBookId(wordBookId);
                wordBookWord.setWordId(wordId);
                wordBookWordRepository.save(wordBookWord);
            }
        }

        updateWordCount(wordBookId);
    }

    @Transactional
    public void removeWordsFromWordBook(Long wordBookId, List<Long> wordIds) {
        if (!wordBookRepository.existsById(wordBookId)) {
            throw new RuntimeException("词书不存在");
        }

        wordBookWordRepository.deleteByWordBookIdAndWordIdIn(wordBookId, wordIds);
        updateWordCount(wordBookId);
    }

    @Transactional
    public void addWordBookToUser(Long userId, Long wordBookId) {
        if (userWordBookRepository.existsByUserIdAndWordBookId(userId, wordBookId)) {
            throw new RuntimeException("该词书已在学习计划中");
        }

        WordBook wordBook = wordBookRepository.findById(wordBookId)
                .orElseThrow(() -> new RuntimeException("词书不存在"));

        UserWordBook userWordBook = new UserWordBook();
        userWordBook.setUserId(userId);
        userWordBook.setWordBookId(wordBookId);
        userWordBook.setMasteredCount(0);

        userWordBookRepository.save(userWordBook);
    }

    @Transactional
    public void removeWordBookFromUser(Long userId, Long wordBookId) {
        if (!userWordBookRepository.existsByUserIdAndWordBookId(userId, wordBookId)) {
            throw new RuntimeException("该词书不在学习计划中");
        }
        userWordBookRepository.deleteByUserIdAndWordBookId(userId, wordBookId);
    }

    public List<WordBookDTO.UserWordBookResponse> getUserWordBooks(Long userId) {
        List<UserWordBook> userWordBooks = userWordBookRepository.findByUserId(userId);
        List<WordBookDTO.UserWordBookResponse> result = new ArrayList<>();

        for (UserWordBook uwb : userWordBooks) {
            WordBook wordBook = wordBookRepository.findById(uwb.getWordBookId()).orElse(null);
            if (wordBook != null) {
                Long masteredCount = userWordBookRepository.countMasteredWordsByUserIdAndWordBookId(userId, wordBook.getId());
                int totalWords = wordBook.getWordCount() != null ? wordBook.getWordCount() : 0;
                int mastered = masteredCount != null ? masteredCount.intValue() : 0;
                double progress = totalWords > 0 ? (double) mastered / totalWords * 100 : 0.0;

                result.add(WordBookDTO.UserWordBookResponse.builder()
                        .id(wordBook.getId())
                        .name(wordBook.getName())
                        .description(wordBook.getDescription())
                        .coverImage(wordBook.getCoverImage())
                        .difficultyLevel(wordBook.getDifficultyLevel().name())
                        .wordCount(totalWords)
                        .masteredCount(mastered)
                        .progress(Math.round(progress * 10.0) / 10.0)
                        .createdAt(uwb.getCreatedAt())
                        .build());
            }
        }

        return result;
    }

    public List<WordBookDTO.ProgressResponse> getWordBookProgress(Long userId) {
        List<WordBookDTO.UserWordBookResponse> userWordBooks = getUserWordBooks(userId);
        return userWordBooks.stream()
                .map(uwb -> WordBookDTO.ProgressResponse.builder()
                        .wordBookId(uwb.getId())
                        .wordBookName(uwb.getName())
                        .totalWords(uwb.getWordCount())
                        .masteredWords(uwb.getMasteredCount())
                        .progress(uwb.getProgress())
                        .build())
                .collect(Collectors.toList());
    }

    private void updateWordCount(Long wordBookId) {
        WordBook wordBook = wordBookRepository.findById(wordBookId).orElse(null);
        if (wordBook != null) {
            Long count = wordBookWordRepository.countByWordBookId(wordBookId);
            wordBook.setWordCount(count != null ? count.intValue() : 0);
            wordBookRepository.save(wordBook);
        }
    }

    private WordBookDTO.Response convertToDTO(WordBook wordBook) {
        return WordBookDTO.Response.builder()
                .id(wordBook.getId())
                .name(wordBook.getName())
                .description(wordBook.getDescription())
                .coverImage(wordBook.getCoverImage())
                .difficultyLevel(wordBook.getDifficultyLevel().name())
                .wordCount(wordBook.getWordCount())
                .createdAt(wordBook.getCreatedAt())
                .updatedAt(wordBook.getUpdatedAt())
                .build();
    }
}
