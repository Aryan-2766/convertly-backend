package com.convertly.dto;

import java.util.List;

/**
 * Metadata for tools supported in the Convertly platform.
 */
public class ToolMetadataDto {

    private String id;
    private String name;
    private String description;
    private String category; // "IMAGES", "PDFS", "DOCUMENTS", "AUDIO", "VIDEO"
    private String phase;    // "MVP", "PHASE_2"
    private List<String> supportedInputFormats;
    private List<String> supportedOutputFormats;
    private boolean available;

    public ToolMetadataDto() {
    }

    public ToolMetadataDto(String id, String name, String description, String category,
                           String phase, List<String> supportedInputFormats,
                           List<String> supportedOutputFormats, boolean available) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.phase = phase;
        this.supportedInputFormats = supportedInputFormats;
        this.supportedOutputFormats = supportedOutputFormats;
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public List<String> getSupportedInputFormats() {
        return supportedInputFormats;
    }

    public void setSupportedInputFormats(List<String> supportedInputFormats) {
        this.supportedInputFormats = supportedInputFormats;
    }

    public List<String> getSupportedOutputFormats() {
        return supportedOutputFormats;
    }

    public void setSupportedOutputFormats(List<String> supportedOutputFormats) {
        this.supportedOutputFormats = supportedOutputFormats;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
