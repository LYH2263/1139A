package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class StatsDTO {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long totalWords;
        private Long todayReviewCount;
        private Double accuracy;
        private Integer streakDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportRequest {
        private String timeRange;
        private String startDate;
        private String endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportResponse {
        private String startDate;
        private String endDate;
        private Long totalStudyMinutes;
        private Long reviewedWordsCount;
        private Long quizCount;
        private Double averageScore;
        private Map<Integer, Long> proficiencyDistribution;
        private List<WordErrorItem> topErrorWords;
        private Integer continuityScore;
        private String username;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordErrorItem {
        private Long wordId;
        private String word;
        private String meaning;
        private Integer errorCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareRequest {
        private String startDate;
        private String endDate;
        private String reportData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareResponse {
        private String token;
        private String shareUrl;
        private String expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicReportResponse {
        private String reportData;
        private String startDate;
        private String endDate;
        private String username;
        private String createdAt;
    }
}
