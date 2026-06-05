package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class QuizDTO {

    public enum QuizMode {
        CHOICE,
        SPELLING
    }

    public enum AnswerResult {
        CORRECT,
        PARTIAL,
        WRONG,
        TIMEOUT
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartResponse {
        private String quizId;
        private QuizMode mode;
        private List<Question> questions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        private Long wordId;
        private String word;
        private String type;
        private String question;
        private List<String> options;
        private String correctAnswer;
        private String hint;
        private String meaning;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitRequest {
        @NotBlank(message = "测验ID不能为空")
        private String quizId;
        
        @NotNull(message = "答案不能为空")
        private List<Answer> answers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Answer {
        private Long wordId;
        private String answer;
        private Boolean timedOut;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDetail {
        private Long wordId;
        private String word;
        private String meaning;
        private String userAnswer;
        private String correctAnswer;
        private AnswerResult result;
        private String feedback;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitResponse {
        private Integer score;
        private Integer correctCount;
        private Integer partialCount;
        private Integer wrongCount;
        private Integer totalCount;
        private Integer duration;
        private List<WordDTO.Response> wrongWords;
        private List<AnswerDetail> answerDetails;
        private QuizMode mode;
        private Double accuracy;
        private Double choiceAccuracy;
        private Double spellingAccuracy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MistakeItem {
        private Long wordId;
        private String word;
        private String phonetic;
        private String pos;
        private String meaning;
        private String example;
        private Integer errorCount;
        private LocalDateTime lastErrorTime;
        private Double accuracy;
        private Integer totalAttempts;
        private List<AccuracyPoint> accuracyTrend;
        private Boolean inTodayReview;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccuracyPoint {
        private String date;
        private Double accuracy;
        private Integer attempts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MistakesResponse {
        private List<MistakeItem> list;
        private Long total;
        private Integer page;
        private Integer size;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PosDistribution {
        private String pos;
        private String posName;
        private Integer errorCount;
        private Integer totalCount;
        private Double errorRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationTypeDistribution {
        private String relationType;
        private String typeName;
        private Integer errorCount;
        private Integer totalCount;
        private Double errorRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeaknessAnalysisResponse {
        private List<PosDistribution> posDistribution;
        private List<RelationTypeDistribution> relationTypeDistribution;
        private List<String> weakPos;
        private List<String> weakRelationTypes;
        private String overallSuggestion;
    }
}
