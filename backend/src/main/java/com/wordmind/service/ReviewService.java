package com.wordmind.service;

import com.wordmind.dto.ReviewDTO;
import com.wordmind.entity.ReviewRecord;
import com.wordmind.entity.User;
import com.wordmind.entity.Word;
import com.wordmind.repository.ReviewRecordRepository;
import com.wordmind.repository.UserRepository;
import com.wordmind.repository.WordBookWordRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    
    @Autowired
    private ReviewRecordRepository reviewRecordRepository;
    
    @Autowired
    private WordRepository wordRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WordBookWordRepository wordBookWordRepository;
    
    private final Map<String, ReviewSession> sessionStore = new ConcurrentHashMap<>();
    
    private static class ReviewSession {
        Long userId;
        List<Long> queue;
        Set<Long> removedWords;
        Map<Long, Integer> consecutiveKnown;
        Map<Long, Integer> consecutiveUnknown;
        List<ReviewDTO.SubmitRequest> reviewedHistory;
        LocalDateTime startTime;
        int averageTimePerWord = 10;
        
        ReviewSession(Long userId, List<Long> queue) {
            this.userId = userId;
            this.queue = new ArrayList<>(queue);
            this.removedWords = new HashSet<>();
            this.consecutiveKnown = new HashMap<>();
            this.consecutiveUnknown = new HashMap<>();
            this.reviewedHistory = new ArrayList<>();
            this.startTime = LocalDateTime.now();
        }
    }
    
    public ReviewDTO.TodayResponse getTodayReviews(Long userId) {
        List<ReviewRecord> records = reviewRecordRepository.findTodayReviews(userId, LocalDateTime.now());
        
        List<ReviewDTO.Response> list = records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        String sessionId = UUID.randomUUID().toString();
        List<Long> wordIds = list.stream()
                .map(ReviewDTO.Response::getWordId)
                .collect(Collectors.toList());
        sessionStore.put(sessionId, new ReviewSession(userId, wordIds));
        
        User user = userRepository.findById(userId).orElse(null);
        String reviewMode = user != null && user.getReviewMode() != null 
                ? user.getReviewMode().name() 
                : User.ReviewMode.CARD.name();
        
        return ReviewDTO.TodayResponse.builder()
                .list(list)
                .total((long) list.size())
                .sessionId(sessionId)
                .reviewMode(reviewMode)
                .build();
    }

    public ReviewDTO.TodayResponse getWordBookReviews(Long userId, Long wordBookId) {
        List<ReviewRecord> records = reviewRecordRepository.findTodayReviewsByWordBookId(userId, LocalDateTime.now(), wordBookId);

        List<ReviewDTO.Response> list = records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Set<Long> existingWordIds = list.stream()
                .map(ReviewDTO.Response::getWordId)
                .collect(Collectors.toSet());

        List<Word> newWords = reviewRecordRepository.findNewWordsByWordBookId(userId, wordBookId);
        for (Word word : newWords) {
            if (!existingWordIds.contains(word.getId())) {
                list.add(ReviewDTO.Response.builder()
                        .wordId(word.getId())
                        .word(word.getWord())
                        .phonetic(word.getPhonetic())
                        .meaning(word.getMeaning())
                        .example(word.getExample())
                        .proficiency(0)
                        .consecutiveKnown(0)
                        .consecutiveUnknown(0)
                        .build());
            }
        }

        String sessionId = UUID.randomUUID().toString();
        List<Long> wordIds = list.stream()
                .map(ReviewDTO.Response::getWordId)
                .collect(Collectors.toList());
        sessionStore.put(sessionId, new ReviewSession(userId, wordIds));

        User user = userRepository.findById(userId).orElse(null);
        String reviewMode = user != null && user.getReviewMode() != null
                ? user.getReviewMode().name()
                : User.ReviewMode.CARD.name();

        return ReviewDTO.TodayResponse.builder()
                .list(list)
                .total((long) list.size())
                .sessionId(sessionId)
                .reviewMode(reviewMode)
                .build();
    }
    
    @Transactional
    public ReviewDTO.SubmitResponse submitReview(Long userId, ReviewDTO.SubmitRequest request) {
        Word word = wordRepository.findById(request.getWordId())
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        
        ReviewRecord.ReviewResult result = ReviewRecord.ReviewResult.valueOf(request.getResult());
        
        Optional<ReviewRecord> existing = reviewRecordRepository.findByUserIdAndWordId(userId, request.getWordId());
        
        ReviewRecord record;
        if (existing.isPresent()) {
            record = existing.get();
            record.setResult(result);
            record.setProficiency(calculateProficiency(record.getProficiency(), result));
        } else {
            record = new ReviewRecord();
            record.setUserId(userId);
            record.setWordId(request.getWordId());
            record.setResult(result);
            record.setProficiency(calculateProficiency(0, result));
        }
        
        boolean removedFromQueue = false;
        boolean addedToQueueEnd = false;
        LocalDateTime nextReviewTime;
        
        ReviewSession session = request.getSessionId() != null 
                ? sessionStore.get(request.getSessionId()) 
                : null;
        
        if (session != null) {
            Long wordId = request.getWordId();
            
            if (result == ReviewRecord.ReviewResult.KNOWN) {
                session.consecutiveKnown.merge(wordId, 1, Integer::sum);
                session.consecutiveUnknown.put(wordId, 0);
                
                int consecutive = session.consecutiveKnown.getOrDefault(wordId, 0);
                if (consecutive >= 3) {
                    removedFromQueue = true;
                    session.removedWords.add(wordId);
                    session.queue.remove(wordId);
                    nextReviewTime = calculateNextReviewTime(record.getProficiency(), true);
                } else {
                    nextReviewTime = calculateNextReviewTime(record.getProficiency(), false);
                }
            } else if (result == ReviewRecord.ReviewResult.UNKNOWN) {
                session.consecutiveUnknown.merge(wordId, 1, Integer::sum);
                session.consecutiveKnown.put(wordId, 0);
                
                int consecutive = session.consecutiveUnknown.getOrDefault(wordId, 0);
                if (consecutive >= 2) {
                    int occurrences = Collections.frequency(session.queue, wordId);
                    if (occurrences < 2) {
                        addedToQueueEnd = true;
                        session.queue.add(wordId);
                    }
                }
                nextReviewTime = calculateNextReviewTime(record.getProficiency(), false);
            } else {
                session.consecutiveKnown.put(wordId, 0);
                session.consecutiveUnknown.put(wordId, 0);
                nextReviewTime = calculateNextReviewTime(record.getProficiency(), false);
            }
            
            session.queue.remove(wordId);
            session.reviewedHistory.add(request);
            
            long elapsedSeconds = java.time.Duration.between(session.startTime, LocalDateTime.now()).getSeconds();
            if (session.reviewedHistory.size() > 0) {
                session.averageTimePerWord = (int) (elapsedSeconds / session.reviewedHistory.size());
            }
        } else {
            nextReviewTime = calculateNextReviewTime(record.getProficiency(), false);
        }
        
        record.setNextReviewAt(nextReviewTime);
        ReviewRecord saved = reviewRecordRepository.save(record);
        
        ReviewDTO.Response nextWord = null;
        if (session != null && !session.queue.isEmpty()) {
            Long nextWordId = session.queue.get(0);
            Word nextW = wordRepository.findById(nextWordId).orElse(null);
            if (nextW != null) {
                ReviewRecord nextRecord = reviewRecordRepository.findByUserIdAndWordId(userId, nextWordId).orElse(null);
                nextWord = convertToDTO(nextW, nextRecord, 
                        session.consecutiveKnown.getOrDefault(nextWordId, 0),
                        session.consecutiveUnknown.getOrDefault(nextWordId, 0));
            }
        }
        
        return ReviewDTO.SubmitResponse.builder()
                .id(saved.getId())
                .wordId(saved.getWordId())
                .word(word.getWord())
                .meaning(word.getMeaning())
                .result(saved.getResult().name())
                .proficiency(saved.getProficiency())
                .nextReviewAt(saved.getNextReviewAt())
                .createdAt(saved.getCreatedAt())
                .removedFromQueue(removedFromQueue)
                .addedToQueueEnd(addedToQueueEnd)
                .nextWord(nextWord)
                .build();
    }
    
    public ReviewDTO.SessionStats getSessionStats(Long userId, String sessionId) {
        ReviewSession session = sessionStore.get(sessionId);
        if (session == null) {
            return ReviewDTO.SessionStats.builder()
                    .reviewedCount(0)
                    .knownCount(0)
                    .unknownCount(0)
                    .vagueCount(0)
                    .knownRate(0.0)
                    .remainingCount(0)
                    .estimatedRemainingSeconds(0L)
                    .build();
        }
        
        int reviewedCount = session.reviewedHistory.size();
        int knownCount = 0;
        int unknownCount = 0;
        int vagueCount = 0;
        
        for (ReviewDTO.SubmitRequest req : session.reviewedHistory) {
            switch (req.getResult()) {
                case "KNOWN": knownCount++; break;
                case "UNKNOWN": unknownCount++; break;
                case "VAGUE": vagueCount++; break;
            }
        }
        
        double knownRate = reviewedCount > 0 ? (double) knownCount / reviewedCount * 100 : 0;
        int remainingCount = session.queue.size();
        long estimatedRemainingSeconds = (long) remainingCount * session.averageTimePerWord;
        
        return ReviewDTO.SessionStats.builder()
                .reviewedCount(reviewedCount)
                .knownCount(knownCount)
                .unknownCount(unknownCount)
                .vagueCount(vagueCount)
                .knownRate(Math.round(knownRate * 10.0) / 10.0)
                .remainingCount(remainingCount)
                .estimatedRemainingSeconds(estimatedRemainingSeconds)
                .build();
    }
    
    public ReviewDTO.SettingsResponse saveSettings(Long userId, ReviewDTO.SettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        User.ReviewMode mode = User.ReviewMode.valueOf(request.getReviewMode());
        user.setReviewMode(mode);
        userRepository.save(user);
        
        return ReviewDTO.SettingsResponse.builder()
                .reviewMode(mode.name())
                .build();
    }
    
    public ReviewDTO.SettingsResponse getSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        String mode = user.getReviewMode() != null 
                ? user.getReviewMode().name() 
                : User.ReviewMode.CARD.name();
        
        return ReviewDTO.SettingsResponse.builder()
                .reviewMode(mode)
                .build();
    }
    
    private int calculateProficiency(int current, ReviewRecord.ReviewResult result) {
        switch (result) {
            case KNOWN: return Math.min(current + 1, 5);
            case VAGUE: return Math.max(current - 1, 0);
            case UNKNOWN: return 0;
            default: return current;
        }
    }
    
    private LocalDateTime calculateNextReviewTime(int proficiency, boolean doubleInterval) {
        LocalDateTime now = LocalDateTime.now();
        long multiplier = doubleInterval ? 2 : 1;
        
        switch (proficiency) {
            case 0: return now.plusMinutes(5 * multiplier);
            case 1: return now.plusHours(1 * multiplier);
            case 2: return now.plusDays(1 * multiplier);
            case 3: return now.plusDays(3 * multiplier);
            case 4: return now.plusDays(7 * multiplier);
            case 5: return now.plusDays(30 * multiplier);
            default: return now.plusDays(1 * multiplier);
        }
    }
    
    private ReviewDTO.Response convertToDTO(ReviewRecord record) {
        Word word = wordRepository.findById(record.getWordId()).orElse(null);
        
        return ReviewDTO.Response.builder()
                .id(record.getId())
                .wordId(record.getWordId())
                .word(word != null ? word.getWord() : "")
                .phonetic(word != null ? word.getPhonetic() : "")
                .meaning(word != null ? word.getMeaning() : "")
                .example(word != null ? word.getExample() : "")
                .result(record.getResult().name())
                .proficiency(record.getProficiency())
                .nextReviewAt(record.getNextReviewAt())
                .createdAt(record.getCreatedAt())
                .consecutiveKnown(0)
                .consecutiveUnknown(0)
                .build();
    }
    
    private ReviewDTO.Response convertToDTO(Word word, ReviewRecord record, int consecutiveKnown, int consecutiveUnknown) {
        return ReviewDTO.Response.builder()
                .id(record != null ? record.getId() : null)
                .wordId(word.getId())
                .word(word.getWord())
                .phonetic(word.getPhonetic())
                .meaning(word.getMeaning())
                .example(word.getExample())
                .result(record != null ? record.getResult().name() : null)
                .proficiency(record != null ? record.getProficiency() : 0)
                .nextReviewAt(record != null ? record.getNextReviewAt() : null)
                .createdAt(record != null ? record.getCreatedAt() : null)
                .consecutiveKnown(consecutiveKnown)
                .consecutiveUnknown(consecutiveUnknown)
                .build();
    }

    public ReviewDTO.CalendarMonthResponse getCalendarMonth(Long userId, String month) {
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDateTime monthStart = startDate.atStartOfDay();
        LocalDateTime monthEnd = endDate.plusDays(1).atStartOfDay();
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        Map<LocalDate, Set<Long>> reviewedWordIdsByDate = new HashMap<>();
        List<ReviewRecord> monthReviewed = reviewRecordRepository.findByUserIdAndDateRange(userId, monthStart, monthEnd);
        for (ReviewRecord rr : monthReviewed) {
            LocalDate date = rr.getCreatedAt().toLocalDate();
            reviewedWordIdsByDate.computeIfAbsent(date, k -> new HashSet<>()).add(rr.getWordId());
        }

        Map<LocalDate, Integer> reviewedCountByDate = new HashMap<>();
        for (Map.Entry<LocalDate, Set<Long>> entry : reviewedWordIdsByDate.entrySet()) {
            reviewedCountByDate.put(entry.getKey(), entry.getValue().size());
        }

        List<ReviewRecord> monthNextReviews = reviewRecordRepository.findUpcomingReviewsByDateRange(userId, monthStart, monthEnd);

        Map<LocalDate, List<ReviewRecord>> pendingByDate = new HashMap<>();
        Map<LocalDate, List<ReviewRecord>> predictedByDate = new HashMap<>();
        Map<LocalDate, List<ReviewRecord>> missedByDate = new HashMap<>();

        for (ReviewRecord rr : monthNextReviews) {
            if (rr.getNextReviewAt() == null) continue;
            LocalDate nextReviewDate = rr.getNextReviewAt().toLocalDate();

            if (nextReviewDate.isBefore(today)) {
                Set<Long> reviewedIds = reviewedWordIdsByDate.get(nextReviewDate);
                if (reviewedIds == null || !reviewedIds.contains(rr.getWordId())) {
                    missedByDate.computeIfAbsent(nextReviewDate, k -> new ArrayList<>()).add(rr);
                }
            } else if (nextReviewDate.equals(today)) {
                if (rr.getNextReviewAt().isBefore(now)) {
                    Set<Long> reviewedIds = reviewedWordIdsByDate.get(nextReviewDate);
                    if (reviewedIds == null || !reviewedIds.contains(rr.getWordId())) {
                        pendingByDate.computeIfAbsent(nextReviewDate, k -> new ArrayList<>()).add(rr);
                    }
                } else {
                    pendingByDate.computeIfAbsent(nextReviewDate, k -> new ArrayList<>()).add(rr);
                }
            } else {
                predictedByDate.computeIfAbsent(nextReviewDate, k -> new ArrayList<>()).add(rr);
            }
        }

        List<ReviewDTO.CalendarDayStats> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int reviewedCount = reviewedCountByDate.getOrDefault(date, 0);
            int pendingCount = pendingByDate.getOrDefault(date, Collections.emptyList()).size();
            int predictedCount = predictedByDate.getOrDefault(date, Collections.emptyList()).size();
            int missedCount = missedByDate.getOrDefault(date, Collections.emptyList()).size();
            int totalCount = reviewedCount + pendingCount + predictedCount + missedCount;

            days.add(ReviewDTO.CalendarDayStats.builder()
                    .date(date.toString())
                    .reviewedCount(reviewedCount)
                    .pendingCount(pendingCount)
                    .predictedCount(predictedCount)
                    .missedCount(missedCount)
                    .totalCount(totalCount)
                    .build());
        }

        return ReviewDTO.CalendarMonthResponse.builder()
                .month(month)
                .days(days)
                .build();
    }

    public ReviewDTO.CalendarDayDetail getCalendarDayDetail(Long userId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        List<ReviewRecord> dayReviewed = reviewRecordRepository.findByUserIdAndDateRange(userId, dayStart, dayEnd);
        Map<Long, ReviewRecord> latestReviewed = new HashMap<>();
        for (ReviewRecord rr : dayReviewed) {
            Long wordId = rr.getWordId();
            if (!latestReviewed.containsKey(wordId) ||
                rr.getCreatedAt().isAfter(latestReviewed.get(wordId).getCreatedAt())) {
                latestReviewed.put(wordId, rr);
            }
        }
        Set<Long> reviewedWordIds = latestReviewed.keySet();

        List<ReviewDTO.CalendarDayWord> reviewedWords = new ArrayList<>();
        for (ReviewRecord rr : latestReviewed.values()) {
            Word word = wordRepository.findById(rr.getWordId()).orElse(null);
            if (word != null) {
                reviewedWords.add(ReviewDTO.CalendarDayWord.builder()
                        .wordId(word.getId())
                        .word(word.getWord())
                        .phonetic(word.getPhonetic())
                        .meaning(word.getMeaning())
                        .proficiency(rr.getProficiency())
                        .status("REVIEWED")
                        .result(rr.getResult().name())
                        .nextReviewAt(rr.getNextReviewAt())
                        .reviewedAt(rr.getCreatedAt())
                        .build());
            }
        }
        reviewedWords.sort((a, b) -> b.getReviewedAt().compareTo(a.getReviewedAt()));

        List<ReviewDTO.CalendarDayWord> pendingWords = new ArrayList<>();
        List<ReviewDTO.CalendarDayWord> missedWords = new ArrayList<>();

        List<ReviewRecord> dayNextReviews = reviewRecordRepository.findUpcomingReviewsByDateRange(userId, dayStart, dayEnd);

        for (ReviewRecord rr : dayNextReviews) {
            if (rr.getNextReviewAt() == null) continue;
            if (reviewedWordIds.contains(rr.getWordId())) continue;

            Word word = wordRepository.findById(rr.getWordId()).orElse(null);
            if (word == null) continue;

            if (date.isBefore(today)) {
                missedWords.add(ReviewDTO.CalendarDayWord.builder()
                        .wordId(word.getId())
                        .word(word.getWord())
                        .phonetic(word.getPhonetic())
                        .meaning(word.getMeaning())
                        .proficiency(rr.getProficiency())
                        .status("MISSED")
                        .result(null)
                        .nextReviewAt(rr.getNextReviewAt())
                        .reviewedAt(null)
                        .build());
            } else if (date.equals(today)) {
                pendingWords.add(ReviewDTO.CalendarDayWord.builder()
                        .wordId(word.getId())
                        .word(word.getWord())
                        .phonetic(word.getPhonetic())
                        .meaning(word.getMeaning())
                        .proficiency(rr.getProficiency())
                        .status("PENDING")
                        .result(null)
                        .nextReviewAt(rr.getNextReviewAt())
                        .reviewedAt(null)
                        .build());
            } else {
                pendingWords.add(ReviewDTO.CalendarDayWord.builder()
                        .wordId(word.getId())
                        .word(word.getWord())
                        .phonetic(word.getPhonetic())
                        .meaning(word.getMeaning())
                        .proficiency(rr.getProficiency())
                        .status("PREDICTED")
                        .result(null)
                        .nextReviewAt(rr.getNextReviewAt())
                        .reviewedAt(null)
                        .build());
            }
        }

        pendingWords.sort((a, b) -> {
            if (a.getNextReviewAt() == null) return 1;
            if (b.getNextReviewAt() == null) return -1;
            return a.getNextReviewAt().compareTo(b.getNextReviewAt());
        });

        missedWords.sort((a, b) -> {
            if (a.getNextReviewAt() == null) return 1;
            if (b.getNextReviewAt() == null) return -1;
            return a.getNextReviewAt().compareTo(b.getNextReviewAt());
        });

        return ReviewDTO.CalendarDayDetail.builder()
                .date(dateStr)
                .reviewedWords(reviewedWords)
                .pendingWords(pendingWords)
                .missedWords(missedWords)
                .reviewedCount(reviewedWords.size())
                .pendingCount(pendingWords.size())
                .missedCount(missedWords.size())
                .build();
    }
}
