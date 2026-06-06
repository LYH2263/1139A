package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewDTO {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitRequest {
        @NotNull(message = "单词ID不能为空")
        private Long wordId;
        
        @NotNull(message = "复习结果不能为空")
        private String result;
        
        private String sessionId;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitResponse {
        private Long id;
        private Long wordId;
        private String word;
        private String meaning;
        private String result;
        private Integer proficiency;
        private LocalDateTime nextReviewAt;
        private LocalDateTime createdAt;
        private Boolean removedFromQueue;
        private Boolean addedToQueueEnd;
        private Response nextWord;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long wordId;
        private String word;
        private String phonetic;
        private String meaning;
        private String example;
        private String result;
        private Integer proficiency;
        private LocalDateTime nextReviewAt;
        private LocalDateTime createdAt;
        private Integer consecutiveKnown;
        private Integer consecutiveUnknown;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayResponse {
        private List<Response> list;
        private Long total;
        private String sessionId;
        private String reviewMode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsRequest {
        @NotNull(message = "复习模式不能为空")
        private String reviewMode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsResponse {
        private String reviewMode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStats {
        private Integer reviewedCount;
        private Integer knownCount;
        private Integer unknownCount;
        private Integer vagueCount;
        private Double knownRate;
        private Integer remainingCount;
        private Long estimatedRemainingSeconds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarDayWord {
        private Long wordId;
        private String word;
        private String phonetic;
        private String meaning;
        private Integer proficiency;
        private String status;
        private String result;
        private LocalDateTime nextReviewAt;
        private LocalDateTime reviewedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarDayStats {
        private String date;
        private Integer reviewedCount;
        private Integer pendingCount;
        private Integer predictedCount;
        private Integer totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarMonthResponse {
        private String month;
        private List<CalendarDayStats> days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarDayDetail {
        private String date;
        private List<CalendarDayWord> reviewedWords;
        private List<CalendarDayWord> pendingWords;
        private Integer reviewedCount;
        private Integer pendingCount;
    }
}
