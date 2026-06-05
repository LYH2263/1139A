package com.wordmind.service;

import com.wordmind.dto.QuizDTO;
import com.wordmind.dto.WordDTO;
import com.wordmind.entity.QuizRecord;
import com.wordmind.entity.StudyPlan;
import com.wordmind.entity.Word;
import com.wordmind.entity.WordRelation;
import com.wordmind.repository.QuizRecordRepository;
import com.wordmind.repository.StudyPlanRepository;
import com.wordmind.repository.WordRelationRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {
    
    @Autowired
    private WordRepository wordRepository;
    
    @Autowired
    private QuizRecordRepository quizRecordRepository;
    
    @Autowired
    private StudyPlanRepository studyPlanRepository;
    
    @Autowired
    private WordRelationRepository wordRelationRepository;
    
    private final Map<String, QuizSession> quizSessions = new HashMap<>();
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    
    private static final Map<String, String> POS_NAMES = new HashMap<>();
    static {
        POS_NAMES.put("n.", "名词");
        POS_NAMES.put("v.", "动词");
        POS_NAMES.put("adj.", "形容词");
        POS_NAMES.put("adv.", "副词");
        POS_NAMES.put("prep.", "介词");
        POS_NAMES.put("conj.", "连词");
        POS_NAMES.put("pron.", "代词");
        POS_NAMES.put("num.", "数词");
        POS_NAMES.put("art.", "冠词");
        POS_NAMES.put("int.", "感叹词");
    }
    
    private static final Map<String, String> RELATION_TYPE_NAMES = new HashMap<>();
    static {
        RELATION_TYPE_NAMES.put("SYNONYM", "同义词");
        RELATION_TYPE_NAMES.put("ANTONYM", "反义词");
        RELATION_TYPE_NAMES.put("TOPIC", "主题相关");
        RELATION_TYPE_NAMES.put("ROOT", "词根");
        RELATION_TYPE_NAMES.put("PREFIX", "前缀");
        RELATION_TYPE_NAMES.put("SUFFIX", "后缀");
        RELATION_TYPE_NAMES.put("SCENE", "场景相关");
    }
    
    public QuizDTO.StartResponse startQuiz(Long userId, int count) {
        List<Word> allWords = wordRepository.findAll();
        Collections.shuffle(allWords);
        
        List<Word> selectedWords = allWords.stream()
                .limit(Math.min(count, allWords.size()))
                .collect(Collectors.toList());
        
        String quizId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        
        List<QuizDTO.Question> questions = selectedWords.stream()
                .map(word -> generateQuestion(word, allWords))
                .collect(Collectors.toList());
        
        quizSessions.put(quizId, new QuizSession(userId, startTime, selectedWords));
        
        return QuizDTO.StartResponse.builder()
                .quizId(quizId)
                .questions(questions)
                .build();
    }
    
    @Transactional
    public QuizDTO.SubmitResponse submitQuiz(Long userId, QuizDTO.SubmitRequest request) {
        QuizSession session = quizSessions.get(request.getQuizId());
        if (session == null) {
            throw new RuntimeException("测验不存在或已过期");
        }
        
        List<Word> words = session.getWords();
        int correctCount = 0;
        List<Word> wrongWords = new ArrayList<>();
        
        for (int i = 0; i < words.size() && i < request.getAnswers().size(); i++) {
            Word word = words.get(i);
            QuizDTO.Answer answer = request.getAnswers().get(i);
            
            if (isCorrect(word, answer)) {
                correctCount++;
            } else {
                wrongWords.add(word);
            }
        }
        
        int totalCount = words.size();
        int score = totalCount > 0 ? (correctCount * 100 / totalCount) : 0;
        int duration = (int) java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).getSeconds();
        
        // 保存测验记录
        QuizRecord record = new QuizRecord();
        record.setUserId(userId);
        record.setScore(score);
        record.setCorrectCount(correctCount);
        record.setTotalCount(totalCount);
        record.setDuration(duration);
        record.setWrongWordIds(wrongWords.stream()
                .map(Word::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        quizRecordRepository.save(record);
        
        // 清理会话
        quizSessions.remove(request.getQuizId());
        
        return QuizDTO.SubmitResponse.builder()
                .score(score)
                .correctCount(correctCount)
                .totalCount(totalCount)
                .duration(duration)
                .wrongWords(wrongWords.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private QuizDTO.Question generateQuestion(Word word, List<Word> allWords) {
        // 生成选择题：选择正确释义
        List<String> options = new ArrayList<>();
        options.add(word.getMeaning());
        
        // 随机选择3个错误选项
        List<Word> otherWords = allWords.stream()
                .filter(w -> !w.getId().equals(word.getId()))
                .collect(Collectors.toList());
        Collections.shuffle(otherWords);
        
        for (int i = 0; i < Math.min(3, otherWords.size()); i++) {
            options.add(otherWords.get(i).getMeaning());
        }
        
        Collections.shuffle(options);
        
        return QuizDTO.Question.builder()
                .wordId(word.getId())
                .word(word.getWord())
                .type("CHOICE")
                .question("请选择 \"" + word.getWord() + "\" 的正确释义")
                .options(options)
                .correctAnswer(word.getMeaning())
                .build();
    }
    
    private boolean isCorrect(Word word, QuizDTO.Answer answer) {
        return word.getMeaning().equals(answer.getAnswer());
    }
    
    private WordDTO.Response convertToDTO(Word word) {
        return WordDTO.Response.builder()
                .id(word.getId())
                .word(word.getWord())
                .phonetic(word.getPhonetic())
                .pos(word.getPos())
                .meaning(word.getMeaning())
                .example(word.getExample())
                .memoryTip(word.getMemoryTip())
                .build();
    }
    
    public QuizDTO.MistakesResponse getMistakes(Long userId, int page, int size, String pos) {
        List<QuizRecord> records = quizRecordRepository.findAllWithWrongWordsByUserId(userId);
        
        Map<Long, WordMistakeStats> mistakeStats = buildMistakeStats(records);
        
        List<Long> todayReviewWordIds = studyPlanRepository.findByUserId(userId).stream()
                .filter(sp -> sp.getPlanType() == StudyPlan.PlanType.TODAY)
                .map(StudyPlan::getWordId)
                .collect(Collectors.toList());
        
        List<QuizDTO.MistakeItem> allItems = mistakeStats.values().stream()
                .filter(stat -> pos == null || pos.isEmpty() || 
                        (stat.getWord() != null && pos.equals(stat.getWord().getPos())))
                .sorted((a, b) -> Integer.compare(b.getErrorCount(), a.getErrorCount()))
                .map(stat -> convertToMistakeItem(stat, todayReviewWordIds))
                .collect(Collectors.toList());
        
        int start = page * size;
        int end = Math.min(start + size, allItems.size());
        List<QuizDTO.MistakeItem> pageItems = start >= allItems.size() 
                ? Collections.emptyList() 
                : allItems.subList(start, end);
        
        return QuizDTO.MistakesResponse.builder()
                .list(pageItems)
                .total((long) allItems.size())
                .page(page)
                .size(size)
                .build();
    }
    
    private Map<Long, WordMistakeStats> buildMistakeStats(List<QuizRecord> records) {
        Map<Long, WordMistakeStats> stats = new HashMap<>();
        Map<Long, Word> wordCache = new HashMap<>();
        
        for (QuizRecord record : records) {
            LocalDateTime recordTime = record.getCreatedAt();
            LocalDate recordDate = recordTime.toLocalDate();
            
            Set<Long> wrongWordIds = parseWrongWordIds(record.getWrongWordIds());
            Set<Long> allWordIdsInQuiz = new HashSet<>();
            for (int i = 0; i < record.getTotalCount(); i++) {
                allWordIdsInQuiz.add((long) (i + 1));
            }
            
            for (Long wordId : wrongWordIds) {
                Word word = wordCache.computeIfAbsent(wordId, id -> 
                        wordRepository.findById(id).orElse(null));
                
                WordMistakeStats wordStats = stats.computeIfAbsent(wordId, id -> 
                        new WordMistakeStats(word));
                
                wordStats.incrementErrorCount();
                wordStats.updateLastErrorTime(recordTime);
                wordStats.addAttempt(recordDate, false);
            }
        }
        
        for (QuizRecord record : records) {
            LocalDate recordDate = record.getCreatedAt().toLocalDate();
            Set<Long> wrongWordIds = parseWrongWordIds(record.getWrongWordIds());
            Set<Long> allWordIds = parseAllWordIds(record, wrongWordIds);
            
            for (Long wordId : allWordIds) {
                if (!wrongWordIds.contains(wordId)) {
                    WordMistakeStats wordStats = stats.get(wordId);
                    if (wordStats != null) {
                        wordStats.addAttempt(recordDate, true);
                    }
                }
            }
        }
        
        return stats;
    }
    
    private Set<Long> parseWrongWordIds(String wrongWordIds) {
        if (wrongWordIds == null || wrongWordIds.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(wrongWordIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }
    
    private Set<Long> parseAllWordIds(QuizRecord record, Set<Long> wrongWordIds) {
        Set<Long> allIds = new HashSet<>(wrongWordIds);
        return allIds;
    }
    
    private QuizDTO.MistakeItem convertToMistakeItem(WordMistakeStats stats, List<Long> todayReviewWordIds) {
        Word word = stats.getWord();
        if (word == null) return null;
        
        List<QuizDTO.AccuracyPoint> trend = buildAccuracyTrend(stats.getDailyAttempts());
        double accuracy = stats.getTotalAttempts() > 0 
                ? (double) stats.getCorrectCount() / stats.getTotalAttempts() * 100 
                : 0.0;
        
        return QuizDTO.MistakeItem.builder()
                .wordId(word.getId())
                .word(word.getWord())
                .phonetic(word.getPhonetic())
                .pos(word.getPos())
                .meaning(word.getMeaning())
                .example(word.getExample())
                .errorCount(stats.getErrorCount())
                .lastErrorTime(stats.getLastErrorTime())
                .accuracy(Math.round(accuracy * 10.0) / 10.0)
                .totalAttempts(stats.getTotalAttempts())
                .accuracyTrend(trend)
                .inTodayReview(todayReviewWordIds.contains(word.getId()))
                .build();
    }
    
    private List<QuizDTO.AccuracyPoint> buildAccuracyTrend(Map<LocalDate, DailyAttempt> dailyAttempts) {
        List<LocalDate> dates = dailyAttempts.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        
        if (dates.isEmpty()) return Collections.emptyList();
        
        LocalDate startDate = dates.get(0);
        LocalDate endDate = LocalDate.now();
        List<QuizDTO.AccuracyPoint> trend = new ArrayList<>();
        
        int totalCorrect = 0;
        int totalAttempts = 0;
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyAttempt attempt = dailyAttempts.get(date);
            if (attempt != null) {
                totalCorrect += attempt.getCorrect();
                totalAttempts += attempt.getTotal();
            }
            
            double accuracy = totalAttempts > 0 
                    ? (double) totalCorrect / totalAttempts * 100 
                    : 0.0;
            
            trend.add(QuizDTO.AccuracyPoint.builder()
                    .date(date.format(DATE_FORMATTER))
                    .accuracy(Math.round(accuracy * 10.0) / 10.0)
                    .attempts(totalAttempts)
                    .build());
        }
        
        int maxPoints = 14;
        if (trend.size() > maxPoints) {
            int step = trend.size() / maxPoints;
            List<QuizDTO.AccuracyPoint> sampled = new ArrayList<>();
            for (int i = 0; i < trend.size(); i += step) {
                sampled.add(trend.get(i));
            }
            if (!sampled.get(sampled.size() - 1).equals(trend.get(trend.size() - 1))) {
                sampled.add(trend.get(trend.size() - 1));
            }
            return sampled;
        }
        
        return trend;
    }
    
    public QuizDTO.WeaknessAnalysisResponse getWeaknessAnalysis(Long userId) {
        List<QuizRecord> records = quizRecordRepository.findAllWithWrongWordsByUserId(userId);
        Map<Long, WordMistakeStats> mistakeStats = buildMistakeStats(records);
        
        Map<String, PosStats> posStatsMap = new HashMap<>();
        Map<String, RelationTypeStats> relationStatsMap = new HashMap<>();
        
        int totalWords = 0;
        int totalErrors = 0;
        
        for (WordMistakeStats stats : mistakeStats.values()) {
            Word word = stats.getWord();
            if (word == null) continue;
            
            String pos = word.getPos() != null ? word.getPos() : "unknown";
            int errorCount = stats.getErrorCount();
            int attemptCount = stats.getTotalAttempts();
            
            posStatsMap.computeIfAbsent(pos, k -> new PosStats())
                    .add(errorCount, attemptCount);
            
            totalErrors += errorCount;
            totalWords++;
            
            List<WordRelation> relations = wordRelationRepository.findByWordId(word.getId());
            for (WordRelation relation : relations) {
                String relType = relation.getRelationType().name();
                relationStatsMap.computeIfAbsent(relType, k -> new RelationTypeStats())
                        .add(errorCount, 1);
            }
        }
        
        List<QuizDTO.PosDistribution> posDistribution = posStatsMap.entrySet().stream()
                .map(e -> {
                    PosStats ps = e.getValue();
                    double errorRate = ps.getTotal() > 0 
                            ? (double) ps.getErrors() / ps.getTotal() * 100 
                            : 0.0;
                    return QuizDTO.PosDistribution.builder()
                            .pos(e.getKey())
                            .posName(POS_NAMES.getOrDefault(e.getKey(), e.getKey()))
                            .errorCount(ps.getErrors())
                            .totalCount(ps.getTotal())
                            .errorRate(Math.round(errorRate * 10.0) / 10.0)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getErrorRate(), a.getErrorRate()))
                .collect(Collectors.toList());
        
        List<QuizDTO.RelationTypeDistribution> relationTypeDistribution = relationStatsMap.entrySet().stream()
                .map(e -> {
                    RelationTypeStats rs = e.getValue();
                    double errorRate = rs.getTotal() > 0 
                            ? (double) rs.getErrors() / rs.getTotal() * 100 
                            : 0.0;
                    return QuizDTO.RelationTypeDistribution.builder()
                            .relationType(e.getKey())
                            .typeName(RELATION_TYPE_NAMES.getOrDefault(e.getKey(), e.getKey()))
                            .errorCount(rs.getErrors())
                            .totalCount(rs.getTotal())
                            .errorRate(Math.round(errorRate * 10.0) / 10.0)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getErrorRate(), a.getErrorRate()))
                .collect(Collectors.toList());
        
        List<String> weakPos = posDistribution.stream()
                .filter(p -> p.getErrorRate() > 50)
                .map(QuizDTO.PosDistribution::getPosName)
                .limit(3)
                .collect(Collectors.toList());
        
        List<String> weakRelationTypes = relationTypeDistribution.stream()
                .filter(r -> r.getErrorRate() > 50)
                .map(QuizDTO.RelationTypeDistribution::getTypeName)
                .limit(3)
                .collect(Collectors.toList());
        
        String overallSuggestion = buildSuggestion(weakPos, weakRelationTypes, totalErrors, totalWords);
        
        return QuizDTO.WeaknessAnalysisResponse.builder()
                .posDistribution(posDistribution)
                .relationTypeDistribution(relationTypeDistribution)
                .weakPos(weakPos)
                .weakRelationTypes(weakRelationTypes)
                .overallSuggestion(overallSuggestion)
                .build();
    }
    
    private String buildSuggestion(List<String> weakPos, List<String> weakRelationTypes, 
                                   int totalErrors, int totalWords) {
        StringBuilder sb = new StringBuilder();
        
        if (totalWords == 0) {
            return "暂无错题数据，建议多做几次测验来生成分析报告。";
        }
        
        sb.append(String.format("你共有 %d 个错题单词，累计错误 %d 次。", totalWords, totalErrors));
        
        if (!weakPos.isEmpty()) {
            sb.append(String.format("在词性方面，%s 的错误率较高，建议重点复习。", 
                    String.join("、", weakPos)));
        }
        
        if (!weakRelationTypes.isEmpty()) {
            sb.append(String.format("在词汇关系方面，%s 相关的单词容易出错，建议加强关联记忆。", 
                    String.join("、", weakRelationTypes)));
        }
        
        if (weakPos.isEmpty() && weakRelationTypes.isEmpty()) {
            sb.append("你的表现比较均衡，继续保持！建议定期复习错题本中的单词。");
        }
        
        return sb.toString();
    }
    
    private static class WordMistakeStats {
        private final Word word;
        private int errorCount;
        private LocalDateTime lastErrorTime;
        private int correctCount;
        private final Map<LocalDate, DailyAttempt> dailyAttempts = new LinkedHashMap<>();
        
        public WordMistakeStats(Word word) {
            this.word = word;
        }
        
        public void incrementErrorCount() {
            this.errorCount++;
        }
        
        public void updateLastErrorTime(LocalDateTime time) {
            if (this.lastErrorTime == null || time.isAfter(this.lastErrorTime)) {
                this.lastErrorTime = time;
            }
        }
        
        public void addAttempt(LocalDate date, boolean correct) {
            DailyAttempt attempt = dailyAttempts.computeIfAbsent(date, k -> new DailyAttempt());
            attempt.addAttempt(correct);
            if (correct) correctCount++;
        }
        
        public int getErrorCount() { return errorCount; }
        public LocalDateTime getLastErrorTime() { return lastErrorTime; }
        public Word getWord() { return word; }
        public Map<LocalDate, DailyAttempt> getDailyAttempts() { return dailyAttempts; }
        public int getCorrectCount() { return correctCount; }
        public int getTotalAttempts() {
            return dailyAttempts.values().stream()
                    .mapToInt(DailyAttempt::getTotal)
                    .sum();
        }
    }
    
    private static class DailyAttempt {
        private int correct;
        private int total;
        
        public void addAttempt(boolean isCorrect) {
            total++;
            if (isCorrect) correct++;
        }
        
        public int getCorrect() { return correct; }
        public int getTotal() { return total; }
    }
    
    private static class PosStats {
        private int errors;
        private int total;
        
        public void add(int errors, int total) {
            this.errors += errors;
            this.total += total;
        }
        
        public int getErrors() { return errors; }
        public int getTotal() { return total; }
    }
    
    private static class RelationTypeStats {
        private int errors;
        private int total;
        
        public void add(int errors, int total) {
            this.errors += errors;
            this.total += total;
        }
        
        public int getErrors() { return errors; }
        public int getTotal() { return total; }
    }
    
    private static class QuizSession {
        private final Long userId;
        private final LocalDateTime startTime;
        private final List<Word> words;
        
        public QuizSession(Long userId, LocalDateTime startTime, List<Word> words) {
            this.userId = userId;
            this.startTime = startTime;
            this.words = words;
        }
        
        public Long getUserId() { return userId; }
        public LocalDateTime getStartTime() { return startTime; }
        public List<Word> getWords() { return words; }
    }
}
