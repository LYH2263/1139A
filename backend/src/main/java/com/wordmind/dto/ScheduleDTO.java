package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;

public class ScheduleDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "计划名称不能为空")
        private String name;

        @NotEmpty(message = "请选择要学习的单词")
        private List<Long> targetWordIds;

        @NotNull(message = "每日学习量不能为空")
        @Positive(message = "每日学习量必须大于0")
        private Integer dailyCount;

        @NotNull(message = "开始日期不能为空")
        private LocalDate startDate;

        @NotNull(message = "结束日期不能为空")
        private LocalDate endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private List<Long> targetWordIds;
        private Integer dailyCount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private Integer totalWords;
        private Integer dailyCount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private String createdAt;
        private Integer completedDays;
        private Integer totalDays;
        private Double progress;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResponse {
        private Long id;
        private String name;
        private Integer totalWords;
        private Integer dailyCount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private String createdAt;
        private Integer completedDays;
        private Integer totalDays;
        private Double progress;
        private List<Long> targetWordIds;
        private List<GanttBar> ganttData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayResponse {
        private Long scheduleId;
        private String scheduleName;
        private LocalDate date;
        private List<Word> plannedWords;
        private List<Long> completedWordIds;
        private boolean completed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Word {
        private Long id;
        private String word;
        private String phonetic;
        private String meaning;
        private String example;
        private boolean isReview;
        private Integer reviewDay;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteRequest {
        private List<Long> completedWordIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GanttBar {
        private LocalDate date;
        private Integer dayIndex;
        private Integer newWordCount;
        private Integer reviewWordCount;
        private Integer totalCount;
        private Integer completedCount;
        private boolean completed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> activeSchedules;
        private List<Response> completedSchedules;
        private List<Response> pausedSchedules;
    }
}
