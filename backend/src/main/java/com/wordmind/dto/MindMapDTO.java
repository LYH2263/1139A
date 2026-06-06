package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class MindMapDTO {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private WordNode centerWord;
        private List<WordNode> nodes;
        private List<RelationEdge> edges;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordNode {
        private Long id;
        private String word;
        private String meaning;
        private String category;
        private Integer depth;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationEdge {
        private Long id;
        private Long source;
        private Long target;
        private String relationType;
        private String label;
        private Long createdBy;
        private String createdByUsername;
        private String status;
        private Boolean isUserContribution;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationRequest {
        @NotNull(message = "源单词ID不能为空")
        private Long sourceWordId;
        
        @NotNull(message = "目标单词ID不能为空")
        private Long targetWordId;
        
        @NotNull(message = "关系类型不能为空")
        private String relationType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingRelationItem {
        private Long id;
        private Long sourceWordId;
        private String sourceWord;
        private Long targetWordId;
        private String targetWord;
        private String relationType;
        private String relationLabel;
        private Long createdBy;
        private String createdByUsername;
        private LocalDateTime createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationReviewRequest {
        @NotNull(message = "审核结果不能为空")
        private Boolean approved;
        
        private String rejectReason;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningPathResponse {
        private List<PathWordNode> nodes;
        private List<PathEdge> edges;
        private List<PathWordNode> unmasteredNodes;
        private List<PathEdge> recommendedPath;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathWordNode {
        private Long id;
        private String word;
        private String meaning;
        private String category;
        private Integer proficiency;
        private Boolean mastered;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathEdge {
        private Long source;
        private Long target;
        private String relationType;
        private String label;
        private Integer weight;
    }
}
