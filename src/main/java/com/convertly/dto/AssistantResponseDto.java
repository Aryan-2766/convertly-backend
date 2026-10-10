package com.convertly.dto;

import java.util.List;

public class AssistantResponseDto {

    private String query;
    private String recommendedFormat;
    private String title;
    private String explanation;
    private List<String> tips;
    private String source; // "LOCAL_RULE_ENGINE"

    public AssistantResponseDto() {}

    public AssistantResponseDto(String query, String recommendedFormat, String title, String explanation, List<String> tips, String source) {
        this.query = query;
        this.recommendedFormat = recommendedFormat;
        this.title = title;
        this.explanation = explanation;
        this.tips = tips;
        this.source = source;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getRecommendedFormat() {
        return recommendedFormat;
    }

    public void setRecommendedFormat(String recommendedFormat) {
        this.recommendedFormat = recommendedFormat;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<String> getTips() {
        return tips;
    }

    public void setTips(List<String> tips) {
        this.tips = tips;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
