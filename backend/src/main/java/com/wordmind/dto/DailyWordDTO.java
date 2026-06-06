package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class DailyWordDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long wordId;
        private String word;
        private String phonetic;
        private String pos;
        private String meaning;
        private String example;
        private String memoryTip;
        private List<RelatedWord> relatedWords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedWord {
        private Long wordId;
        private String word;
        private String meaning;
        private String relationType;
        private String relationTypeName;
    }
}
