package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class WordBookDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "词书名称不能为空")
        @Size(max = 100, message = "词书名称长度不能超过100字符")
        private String name;

        @Size(max = 500, message = "描述长度不能超过500字符")
        private String description;

        @Size(max = 500, message = "封面图片URL长度不能超过500字符")
        private String coverImage;

        @NotNull(message = "难度等级不能为空")
        private String difficultyLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 100, message = "词书名称长度不能超过100字符")
        private String name;

        @Size(max = 500, message = "描述长度不能超过500字符")
        private String description;

        @Size(max = 500, message = "封面图片URL长度不能超过500字符")
        private String coverImage;

        private String difficultyLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private String coverImage;
        private String difficultyLevel;
        private Integer wordCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> list;
        private Long total;
        private Integer page;
        private Integer size;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserWordBookResponse {
        private Long id;
        private String name;
        private String description;
        private String coverImage;
        private String difficultyLevel;
        private Integer wordCount;
        private Integer masteredCount;
        private Double progress;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordBatchRequest {
        private List<Long> wordIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressResponse {
        private Long wordBookId;
        private String wordBookName;
        private Integer totalWords;
        private Integer masteredWords;
        private Double progress;
    }
}
