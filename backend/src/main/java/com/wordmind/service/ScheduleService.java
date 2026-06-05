package com.wordmind.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordmind.dto.ScheduleDTO;
import com.wordmind.entity.ScheduleProgress;
import com.wordmind.entity.StudySchedule;
import com.wordmind.entity.Word;
import com.wordmind.repository.ScheduleProgressRepository;
import com.wordmind.repository.StudyScheduleRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private StudyScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleProgressRepository progressRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int[] EBINGHAUS_INTERVALS = {1, 2, 4, 7, 15, 30};

    @Transactional
    public ScheduleDTO.Response createSchedule(Long userId, ScheduleDTO.CreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("结束日期不能早于开始日期");
        }

        int totalDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        int totalWords = request.getTargetWordIds().size();
        int maxCapacity = totalDays * request.getDailyCount();

        if (totalWords > maxCapacity) {
            throw new RuntimeException(String.format("所选单词数量(%d)超出计划周期容量(%d)，请减少单词或增加每日学习量", 
                    totalWords, maxCapacity));
        }

        StudySchedule schedule = new StudySchedule();
        schedule.setUserId(userId);
        schedule.setName(request.getName());
        schedule.setTargetWordIds(serializeIds(request.getTargetWordIds()));
        schedule.setDailyCount(request.getDailyCount());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setStatus(StudySchedule.ScheduleStatus.ACTIVE);

        StudySchedule saved = scheduleRepository.save(schedule);
        generateDailyProgress(saved, request.getTargetWordIds());

        return convertToResponse(saved);
    }

    @Transactional
    public ScheduleDTO.Response updateSchedule(Long userId, Long id, ScheduleDTO.UpdateRequest request) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("计划不存在或无权限"));

        if (request.getName() != null) {
            schedule.setName(request.getName());
        }
        if (request.getDailyCount() != null) {
            schedule.setDailyCount(request.getDailyCount());
        }
        if (request.getStartDate() != null) {
            schedule.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            schedule.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            schedule.setStatus(StudySchedule.ScheduleStatus.valueOf(request.getStatus()));
        }
        if (request.getTargetWordIds() != null) {
            schedule.setTargetWordIds(serializeIds(request.getTargetWordIds()));
            progressRepository.deleteByScheduleId(schedule.getId());
            generateDailyProgress(schedule, request.getTargetWordIds());
        }

        return convertToResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public void deleteSchedule(Long userId, Long id) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("计划不存在或无权限"));
        progressRepository.deleteByScheduleId(id);
        scheduleRepository.delete(schedule);
    }

    public ScheduleDTO.ListResponse getSchedules(Long userId) {
        List<StudySchedule> all = scheduleRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return ScheduleDTO.ListResponse.builder()
                .activeSchedules(filterAndConvert(all, StudySchedule.ScheduleStatus.ACTIVE))
                .completedSchedules(filterAndConvert(all, StudySchedule.ScheduleStatus.COMPLETED))
                .pausedSchedules(filterAndConvert(all, StudySchedule.ScheduleStatus.PAUSED))
                .build();
    }

    public ScheduleDTO.DetailResponse getScheduleDetail(Long userId, Long id) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("计划不存在或无权限"));

        List<Long> targetWordIds = deserializeIds(schedule.getTargetWordIds());
        List<ScheduleProgress> progressList = progressRepository.findByScheduleIdOrderByDateAsc(schedule.getId());
        List<ScheduleDTO.GanttBar> ganttData = generateGanttData(schedule, progressList);

        int totalDays = (int) ChronoUnit.DAYS.between(schedule.getStartDate(), schedule.getEndDate()) + 1;
        Long completedDays = progressRepository.countCompletedDays(schedule.getId());
        double progress = targetWordIds.size() > 0 ? 
                (double) getTotalCompletedWords(progressList) / targetWordIds.size() * 100 : 0;

        return ScheduleDTO.DetailResponse.builder()
                .id(schedule.getId())
                .name(schedule.getName())
                .totalWords(targetWordIds.size())
                .dailyCount(schedule.getDailyCount())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .status(schedule.getStatus().name())
                .createdAt(schedule.getCreatedAt().toString())
                .completedDays(completedDays != null ? completedDays.intValue() : 0)
                .totalDays(totalDays)
                .progress(Math.round(progress * 10.0) / 10.0)
                .targetWordIds(targetWordIds)
                .ganttData(ganttData)
                .build();
    }

    public ScheduleDTO.TodayResponse getTodaySchedule(Long userId, Long id) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("计划不存在或无权限"));

        LocalDate today = LocalDate.now();
        ScheduleProgress progress = progressRepository.findByScheduleIdAndDate(id, today)
                .orElseThrow(() -> new RuntimeException("今日无学习计划"));

        List<Long> plannedWordIds = deserializeIds(progress.getPlannedWordIds());
        List<Long> completedWordIds = progress.getCompletedWordIds() != null ?
                deserializeIds(progress.getCompletedWordIds()) : new ArrayList<>();

        Map<Long, Integer> wordDayMap = buildWordDayMap(schedule);
        List<ScheduleDTO.Word> words = buildWordList(plannedWordIds, wordDayMap, today, schedule.getStartDate());

        return ScheduleDTO.TodayResponse.builder()
                .scheduleId(schedule.getId())
                .scheduleName(schedule.getName())
                .date(today)
                .plannedWords(words)
                .completedWordIds(completedWordIds)
                .completed(completedWordIds.containsAll(plannedWordIds))
                .build();
    }

    public List<ScheduleDTO.TodayResponse> getTodayAllSchedules(Long userId) {
        LocalDate today = LocalDate.now();
        List<StudySchedule> activeSchedules = scheduleRepository.findActiveSchedulesForDate(userId, today);

        return activeSchedules.stream()
                .map(schedule -> {
                    try {
                        return getTodaySchedule(userId, schedule.getId());
                    } catch (RuntimeException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleDTO.TodayResponse completeToday(Long userId, Long id, ScheduleDTO.CompleteRequest request) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("计划不存在或无权限"));

        LocalDate today = LocalDate.now();
        ScheduleProgress progress = progressRepository.findByScheduleIdAndDate(id, today)
                .orElseThrow(() -> new RuntimeException("今日无学习计划"));

        List<Long> plannedWordIds = deserializeIds(progress.getPlannedWordIds());
        if (!plannedWordIds.containsAll(request.getCompletedWordIds())) {
            throw new RuntimeException("存在不属于今日计划的单词");
        }

        Set<Long> existingCompleted = progress.getCompletedWordIds() != null ?
                new HashSet<>(deserializeIds(progress.getCompletedWordIds())) : new HashSet<>();
        existingCompleted.addAll(request.getCompletedWordIds());
        progress.setCompletedWordIds(serializeIds(new ArrayList<>(existingCompleted)));
        progressRepository.save(progress);

        checkAndUpdateScheduleStatus(schedule);

        return getTodaySchedule(userId, id);
    }

    private void generateDailyProgress(StudySchedule schedule, List<Long> targetWordIds) {
        LocalDate startDate = schedule.getStartDate();
        LocalDate endDate = schedule.getEndDate();
        int dailyCount = schedule.getDailyCount();

        Map<LocalDate, List<Long>> dailyAssignments = new LinkedHashMap<>();
        Map<Long, LocalDate> wordLearnDate = new HashMap<>();

        int wordIndex = 0;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate) && wordIndex < targetWordIds.size()) {
            List<Long> dayWords = new ArrayList<>();
            int newWordsNeeded = Math.min(dailyCount, targetWordIds.size() - wordIndex);

            for (int i = 0; i < newWordsNeeded; i++) {
                Long wordId = targetWordIds.get(wordIndex);
                dayWords.add(wordId);
                wordLearnDate.put(wordId, currentDate);
                wordIndex++;
            }

            dailyAssignments.put(currentDate, dayWords);
            currentDate = currentDate.plusDays(1);
        }

        for (Map.Entry<Long, LocalDate> entry : wordLearnDate.entrySet()) {
            Long wordId = entry.getKey();
            LocalDate learnDate = entry.getValue();

            for (int interval : EBINGHAUS_INTERVALS) {
                LocalDate reviewDate = learnDate.plusDays(interval);
                if (!reviewDate.isAfter(endDate)) {
                    dailyAssignments.computeIfAbsent(reviewDate, k -> new ArrayList<>());
                    dailyAssignments.get(reviewDate).add(wordId);
                }
            }
        }

        for (Map.Entry<LocalDate, List<Long>> entry : dailyAssignments.entrySet()) {
            if (entry.getKey().isBefore(startDate) || entry.getKey().isAfter(endDate)) {
                continue;
            }

            List<Long> orderedWords = interleaveNewAndReview(entry.getValue(), wordLearnDate, entry.getKey());

            ScheduleProgress progress = new ScheduleProgress();
            progress.setScheduleId(schedule.getId());
            progress.setDate(entry.getKey());
            progress.setPlannedWordIds(serializeIds(orderedWords));
            progressRepository.save(progress);
        }
    }

    private List<Long> interleaveNewAndReview(List<Long> words, Map<Long, LocalDate> wordLearnDate, LocalDate currentDate) {
        List<Long> newWords = new ArrayList<>();
        List<Long> reviewWords = new ArrayList<>();

        for (Long wordId : words) {
            LocalDate learnDate = wordLearnDate.get(wordId);
            if (learnDate != null && learnDate.equals(currentDate)) {
                newWords.add(wordId);
            } else {
                reviewWords.add(wordId);
            }
        }

        List<Long> result = new ArrayList<>();
        int newIdx = 0, reviewIdx = 0;

        while (newIdx < newWords.size() || reviewIdx < reviewWords.size()) {
            if (reviewIdx < reviewWords.size()) {
                result.add(reviewWords.get(reviewIdx++));
            }
            if (newIdx < newWords.size()) {
                result.add(newWords.get(newIdx++));
            }
        }

        return result;
    }

    private Map<Long, Integer> buildWordDayMap(StudySchedule schedule) {
        Map<Long, Integer> wordDayMap = new HashMap<>();
        List<Long> targetWordIds = deserializeIds(schedule.getTargetWordIds());
        LocalDate startDate = schedule.getStartDate();
        int dailyCount = schedule.getDailyCount();

        for (int i = 0; i < targetWordIds.size(); i++) {
            int dayIndex = i / dailyCount;
            wordDayMap.put(targetWordIds.get(i), dayIndex + 1);
        }

        return wordDayMap;
    }

    private List<ScheduleDTO.Word> buildWordList(List<Long> wordIds, Map<Long, Integer> wordDayMap, 
            LocalDate today, LocalDate startDate) {
        List<Word> words = wordRepository.findAllById(wordIds);
        Map<Long, Word> wordMap = words.stream().collect(Collectors.toMap(Word::getId, w -> w));

        int todayIndex = (int) ChronoUnit.DAYS.between(startDate, today) + 1;

        return wordIds.stream()
                .map(id -> {
                    Word w = wordMap.get(id);
                    if (w == null) return null;
                    int learnDay = wordDayMap.getOrDefault(id, 1);
                    boolean isReview = learnDay < todayIndex;
                    return ScheduleDTO.Word.builder()
                            .id(w.getId())
                            .word(w.getWord())
                            .phonetic(w.getPhonetic())
                            .meaning(w.getMeaning())
                            .example(w.getExample())
                            .isReview(isReview)
                            .reviewDay(isReview ? todayIndex - learnDay : 0)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<ScheduleDTO.GanttBar> generateGanttData(StudySchedule schedule, List<ScheduleProgress> progressList) {
        Map<LocalDate, ScheduleProgress> progressMap = progressList.stream()
                .collect(Collectors.toMap(ScheduleProgress::getDate, p -> p));

        Map<Long, LocalDate> wordLearnDate = new HashMap<>();
        List<Long> targetWordIds = deserializeIds(schedule.getTargetWordIds());
        LocalDate startDate = schedule.getStartDate();
        int dailyCount = schedule.getDailyCount();

        for (int i = 0; i < targetWordIds.size(); i++) {
            int dayIndex = i / dailyCount;
            wordLearnDate.put(targetWordIds.get(i), startDate.plusDays(dayIndex));
        }

        List<ScheduleDTO.GanttBar> ganttData = new ArrayList<>();
        LocalDate current = startDate;
        int dayIndex = 1;

        while (!current.isAfter(schedule.getEndDate())) {
            ScheduleProgress progress = progressMap.get(current);
            List<Long> planned = progress != null ? deserializeIds(progress.getPlannedWordIds()) : new ArrayList<>();
            List<Long> completed = progress != null && progress.getCompletedWordIds() != null ?
                    deserializeIds(progress.getCompletedWordIds()) : new ArrayList<>();

            int newWordCount = 0;
            int reviewWordCount = 0;

            for (Long wordId : planned) {
                LocalDate learnDate = wordLearnDate.get(wordId);
                if (learnDate != null && learnDate.equals(current)) {
                    newWordCount++;
                } else {
                    reviewWordCount++;
                }
            }

            ganttData.add(ScheduleDTO.GanttBar.builder()
                    .date(current)
                    .dayIndex(dayIndex)
                    .newWordCount(newWordCount)
                    .reviewWordCount(reviewWordCount)
                    .totalCount(planned.size())
                    .completedCount(completed.size())
                    .completed(completed.containsAll(planned) && !planned.isEmpty())
                    .build());

            current = current.plusDays(1);
            dayIndex++;
        }

        return ganttData;
    }

    private int getTotalCompletedWords(List<ScheduleProgress> progressList) {
        Set<Long> allCompleted = new HashSet<>();
        for (ScheduleProgress progress : progressList) {
            if (progress.getCompletedWordIds() != null) {
                allCompleted.addAll(deserializeIds(progress.getCompletedWordIds()));
            }
        }
        return allCompleted.size();
    }

    private void checkAndUpdateScheduleStatus(StudySchedule schedule) {
        List<Long> targetWordIds = deserializeIds(schedule.getTargetWordIds());
        List<ScheduleProgress> progressList = progressRepository.findByScheduleIdOrderByDateAsc(schedule.getId());
        Set<Long> allCompleted = new HashSet<>();

        for (ScheduleProgress progress : progressList) {
            if (progress.getCompletedWordIds() != null) {
                allCompleted.addAll(deserializeIds(progress.getCompletedWordIds()));
            }
        }

        if (allCompleted.containsAll(targetWordIds)) {
            schedule.setStatus(StudySchedule.ScheduleStatus.COMPLETED);
            scheduleRepository.save(schedule);
        }
    }

    private List<ScheduleDTO.Response> filterAndConvert(List<StudySchedule> schedules, StudySchedule.ScheduleStatus status) {
        return schedules.stream()
                .filter(s -> s.getStatus() == status)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ScheduleDTO.Response convertToResponse(StudySchedule schedule) {
        List<Long> targetWordIds = deserializeIds(schedule.getTargetWordIds());
        int totalDays = (int) ChronoUnit.DAYS.between(schedule.getStartDate(), schedule.getEndDate()) + 1;
        Long completedDays = progressRepository.countCompletedDays(schedule.getId());

        List<ScheduleProgress> progressList = progressRepository.findByScheduleIdOrderByDateAsc(schedule.getId());
        double progress = targetWordIds.size() > 0 ?
                (double) getTotalCompletedWords(progressList) / targetWordIds.size() * 100 : 0;

        return ScheduleDTO.Response.builder()
                .id(schedule.getId())
                .name(schedule.getName())
                .totalWords(targetWordIds.size())
                .dailyCount(schedule.getDailyCount())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .status(schedule.getStatus().name())
                .createdAt(schedule.getCreatedAt().toString())
                .completedDays(completedDays != null ? completedDays.intValue() : 0)
                .totalDays(totalDays)
                .progress(Math.round(progress * 10.0) / 10.0)
                .build();
    }

    private String serializeIds(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            throw new RuntimeException("序列化ID列表失败", e);
        }
    }

    private List<Long> deserializeIds(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            throw new RuntimeException("反序列化ID列表失败", e);
        }
    }
}
