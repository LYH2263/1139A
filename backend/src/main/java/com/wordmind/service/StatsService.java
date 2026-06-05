package com.wordmind.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordmind.dto.StatsDTO;
import com.wordmind.dto.StudyPlanDTO;
import com.wordmind.entity.QuizRecord;
import com.wordmind.entity.SharedReport;
import com.wordmind.entity.StudyPlan;
import com.wordmind.entity.User;
import com.wordmind.entity.Word;
import com.wordmind.repository.QuizRecordRepository;
import com.wordmind.repository.ReviewRecordRepository;
import com.wordmind.repository.SharedReportRepository;
import com.wordmind.repository.StudyPlanRepository;
import com.wordmind.repository.UserRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StatsService {
    
    @Autowired
    private ReviewRecordRepository reviewRecordRepository;
    
    @Autowired
    private QuizRecordRepository quizRecordRepository;
    
    @Autowired
    private StudyPlanRepository studyPlanRepository;
    
    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private WordBookService wordBookService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SharedReportRepository sharedReportRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.frontend.url:http://localhost:3139}")
    private String frontendUrl;
    
    public StatsDTO.Response getUserStats(Long userId) {
        Long totalWords = reviewRecordRepository.countKnownWordsByUserId(userId);
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        Long todayReviewCount = reviewRecordRepository.countTodayReviewsByUserId(userId, startOfDay, endOfDay);
        Double avgScore = quizRecordRepository.calculateAverageScoreByUserId(userId);
        
        return StatsDTO.Response.builder()
                .totalWords(totalWords != null ? totalWords : 0L)
                .todayReviewCount(todayReviewCount != null ? todayReviewCount : 0L)
                .accuracy(avgScore != null ? avgScore : 0.0)
                .streakDays(calculateStreakDays(userId))
                .wordBookProgress(wordBookService.getWordBookProgress(userId))
                .build();
    }
    
    private Integer calculateStreakDays(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        Long todayCount = reviewRecordRepository.countTodayReviewsByUserId(userId, startOfDay, endOfDay);
        return todayCount != null && todayCount > 0 ? 1 : 0;
    }

    @Transactional(readOnly = true)
    public StatsDTO.ReportResponse generateReport(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Long totalDuration = quizRecordRepository.sumDurationByUserIdAndDateRange(userId, startDate, endDate);
        Long totalStudyMinutes = totalDuration != null ? totalDuration / 60 : 0L;

        Long reviewedWordsCount = reviewRecordRepository.countDistinctWordsByUserIdAndDateRange(userId, startDate, endDate);
        if (reviewedWordsCount == null) reviewedWordsCount = 0L;

        Long quizCount = quizRecordRepository.countByUserIdAndDateRange(userId, startDate, endDate);
        if (quizCount == null) quizCount = 0L;

        Double averageScore = quizRecordRepository.calculateAverageScoreByUserIdAndDateRange(userId, startDate, endDate);
        if (averageScore == null) averageScore = 0.0;

        Map<Integer, Long> proficiencyDistribution = getProficiencyDistribution(userId, startDate, endDate);
        List<StatsDTO.WordErrorItem> topErrorWords = getTopErrorWords(userId, startDate, endDate, 10);
        Integer continuityScore = calculateContinuityScore(userId, startDate, endDate);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return StatsDTO.ReportResponse.builder()
                .startDate(startDate.format(formatter))
                .endDate(endDate.format(formatter))
                .totalStudyMinutes(totalStudyMinutes)
                .reviewedWordsCount(reviewedWordsCount)
                .quizCount(quizCount)
                .averageScore(Math.round(averageScore * 10.0) / 10.0)
                .proficiencyDistribution(proficiencyDistribution)
                .topErrorWords(topErrorWords)
                .continuityScore(continuityScore)
                .username(user.getUsername())
                .build();
    }

    private Map<Integer, Long> getProficiencyDistribution(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 0; i <= 5; i++) {
            distribution.put(i, 0L);
        }

        List<Object[]> results = reviewRecordRepository.countProficiencyDistribution(userId, startDate, endDate);
        for (Object[] result : results) {
            Integer proficiency = (Integer) result[0];
            Long count = (Long) result[1];
            if (proficiency >= 0 && proficiency <= 5) {
                distribution.put(proficiency, count);
            }
        }

        return distribution;
    }

    private List<StatsDTO.WordErrorItem> getTopErrorWords(Long userId, LocalDateTime startDate, LocalDateTime endDate, int limit) {
        List<QuizRecord> quizRecords = quizRecordRepository.findWithWrongWordsByUserIdAndDateRange(userId, startDate, endDate);
        Map<Long, Integer> wordErrorCount = new HashMap<>();

        for (QuizRecord record : quizRecords) {
            String wrongWordIds = record.getWrongWordIds();
            if (wrongWordIds != null && !wrongWordIds.isEmpty()) {
                String[] ids = wrongWordIds.split(",");
                for (String idStr : ids) {
                    try {
                        Long wordId = Long.parseLong(idStr.trim());
                        wordErrorCount.merge(wordId, 1, Integer::sum);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return wordErrorCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Word word = wordRepository.findById(entry.getKey()).orElse(null);
                    return StatsDTO.WordErrorItem.builder()
                            .wordId(entry.getKey())
                            .word(word != null ? word.getWord() : "未知单词")
                            .meaning(word != null ? word.getMeaning() : "")
                            .errorCount(entry.getValue())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Integer calculateContinuityScore(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Long activeDays = reviewRecordRepository.countActiveDays(userId, startDate, endDate);
        if (activeDays == null) activeDays = 0L;

        long totalDays = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        if (totalDays <= 0) totalDays = 1;

        double continuityRate = (double) activeDays / totalDays;
        return (int) Math.round(continuityRate * 100);
    }

    @Transactional
    public StatsDTO.ShareResponse createShareToken(Long userId, StatsDTO.ShareRequest request) throws JsonProcessingException {
        LocalDateTime startDate = LocalDateTime.parse(request.getStartDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime endDate = LocalDateTime.parse(request.getEndDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        SharedReport sharedReport = new SharedReport();
        sharedReport.setToken(token);
        sharedReport.setUserId(userId);
        sharedReport.setStartDate(startDate);
        sharedReport.setEndDate(endDate);
        sharedReport.setReportData(request.getReportData());
        sharedReport.setExpiresAt(expiresAt);

        sharedReportRepository.save(sharedReport);

        String shareUrl = frontendUrl + "/public/report/" + token;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return StatsDTO.ShareResponse.builder()
                .token(token)
                .shareUrl(shareUrl)
                .expiresAt(expiresAt.format(formatter))
                .build();
    }

    @Transactional(readOnly = true)
    public StatsDTO.PublicReportResponse getPublicReport(String token) {
        SharedReport sharedReport = sharedReportRepository.findValidByToken(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("分享链接无效或已过期"));

        User user = userRepository.findById(sharedReport.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return StatsDTO.PublicReportResponse.builder()
                .reportData(sharedReport.getReportData())
                .startDate(sharedReport.getStartDate().format(formatter))
                .endDate(sharedReport.getEndDate().format(formatter))
                .username(user.getUsername())
                .createdAt(sharedReport.getCreatedAt().format(formatter))
                .build();
    }
    
    @Transactional
    public StudyPlanDTO.Response createStudyPlan(Long userId, StudyPlanDTO.CreateRequest request) {
        if (studyPlanRepository.existsByUserIdAndWordId(userId, request.getWordId())) {
            throw new RuntimeException("该单词已在学习计划中");
        }
        
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setWordId(request.getWordId());
        plan.setPlanType(StudyPlan.PlanType.valueOf(request.getPlanType()));
        
        StudyPlan saved = studyPlanRepository.save(plan);
        return convertToDTO(saved);
    }
    
    public List<StudyPlanDTO.Response> getUserStudyPlans(Long userId) {
        return studyPlanRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private StudyPlanDTO.Response convertToDTO(StudyPlan plan) {
        Word word = wordRepository.findById(plan.getWordId()).orElse(null);
        
        return StudyPlanDTO.Response.builder()
                .id(plan.getId())
                .wordId(plan.getWordId())
                .word(word != null ? word.getWord() : "")
                .meaning(word != null ? word.getMeaning() : "")
                .planType(plan.getPlanType().name())
                .createdAt(plan.getCreatedAt().toString())
                .build();
    }
}
