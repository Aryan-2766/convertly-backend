package com.convertly.service;

import com.convertly.dto.AssistantResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Intelligent Media & Document Format Advisor.
 * Combines Google Gemini LLM for deep context-aware format recommendations (desktop sizing,
 * legal compliance, aspect ratios, bitrates) with an extensive offline heuristic knowledge base.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    @Value("${convertly.ai.gemini-api-key:}")
    private String geminiApiKey;

    @Value("${convertly.ai.gemini-model:gemini-1.5-flash}")
    private String geminiModel;

    public AssistantResponseDto getRecommendation(String query) {
        if (query == null || query.isBlank()) {
            return getFallbackResponse("Please enter a question regarding formats, dimensions, or documents.");
        }

        String trimmedQuery = query.trim();

        // 1. Try Google Gemini API if API key is provided
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                AssistantResponseDto aiResponse = callGeminiAdvisor(trimmedQuery);
                if (aiResponse != null) {
                    return aiResponse;
                }
            } catch (Exception e) {
                log.warn("Gemini API call failed or timed out, gracefully falling back to offline expert engine: {}", e.getMessage());
            }
        }

        // 2. Intelligent Offline Expert Engine (covers desktop resolutions, legal documents, web, print, video, audio)
        return getExpertRuleRecommendation(trimmedQuery);
    }

    /**
     * Calls Google Gemini Generative Language API with structured JSON output instructions.
     */
    private AssistantResponseDto callGeminiAdvisor(String userQuery) throws Exception {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiModel, geminiApiKey
        );

        String systemInstruction = "You are Convertly's Senior Media & Document Architect. "
                + "Provide expert advice on file formats, desktop/mobile resolutions, legal document standards, photo dimensions, audio bitrates, and video codecs. "
                + "Respond STRICTLY with a valid raw JSON object (no markdown backticks, no code block fences) with these keys: "
                + "recommendedFormat (string, e.g. PDF/A, WEBP, 1920x1080 JPG, MP3, MP4), "
                + "title (string, short punchy summary), "
                + "explanation (string, 2-3 sentences explaining why with specific resolutions or legal/standards context), "
                + "tips (array of 2-4 actionable bullet strings with exact dimensions, DPI, or settings).";

        Map<String, Object> requestPayload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", systemInstruction + "\n\nUser Question: " + userQuery)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 800
                )
        );

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(8))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Gemini API returned status {}: {}", response.statusCode(), response.body());
            return null;
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        JsonNode candidates = rootNode.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
            if (!textNode.isMissingNode()) {
                String rawAiText = textNode.asText().trim();
                return parseAiJsonResponse(rawAiText, userQuery);
            }
        }
        return null;
    }

    private AssistantResponseDto parseAiJsonResponse(String rawAiText, String query) {
        try {
            // Strip any accidental markdown formatting
            String cleanJson = rawAiText;
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode parsed = objectMapper.readTree(cleanJson);
            String recFormat = parsed.path("recommendedFormat").asText("OPTIMIZED");
            String title = parsed.path("title").asText("AI Format Recommendation");
            String explanation = parsed.path("explanation").asText("");

            List<String> tips = new ArrayList<>();
            JsonNode tipsNode = parsed.path("tips");
            if (tipsNode.isArray()) {
                for (JsonNode t : tipsNode) {
                    tips.add(t.asText());
                }
            }

            return new AssistantResponseDto(
                    query,
                    recFormat,
                    title,
                    explanation,
                    tips.isEmpty() ? List.of("Verify visual quality against your target platform.") : tips,
                    "GEMINI_AI"
            );
        } catch (Exception e) {
            log.info("Could not parse AI response as JSON, wrapping text directly: {}", e.getMessage());
            return new AssistantResponseDto(
                    query,
                    "OPTIMIZED",
                    "AI Architectural Recommendation",
                    rawAiText,
                    List.of("Check aspect ratio and target platform requirements before export."),
                    "GEMINI_AI"
            );
        }
    }

    /**
     * Rich Expert Knowledge Base for comprehensive domain questions (legal, desktop wallpapers, web, print).
     */
    private AssistantResponseDto getExpertRuleRecommendation(String query) {
        String q = query.toLowerCase(Locale.ROOT);

        // Desktop Wallpaper / Monitor Photo Resolution
        if (q.contains("desktop") || q.contains("wallpaper") || q.contains("monitor") || q.contains("screen") || q.contains("pc")) {
            return new AssistantResponseDto(
                    query,
                    "1920×1080 (FHD) / 3840×2160 (4K) JPG or WEBP",
                    "Optimal Desktop Display & Wallpaper Sizing",
                    "For standard desktop displays, 1920×1080 px (16:9 aspect ratio) is the most universal resolution. For high-density Retina or 4K monitors, use 3840×2160 px. Save as JPG (quality 90%) for rich photographic gradients or WebP for fast lightweight rendering.",
                    Arrays.asList(
                            "Standard 1080p: 1920 × 1080 px at 16:9 aspect ratio.",
                            "4K UHD / Retina: 3840 × 2160 px for ultra-sharp high-DPI panels.",
                            "Ultrawide monitors: 2560 × 1080 or 3440 × 1440 px (21:9 aspect ratio).",
                            "Color profile: Ensure export is in sRGB for consistent color across browsers and OSs."
                    ),
                    "EXPERT_KNOWLEDGE_ENGINE"
            );
        }

        // Legal / Contracts / Archival Compliance
        if (q.contains("legal") || q.contains("court") || q.contains("contract") || q.contains("compliance") || q.contains("official") || q.contains("law")) {
            return new AssistantResponseDto(
                    query,
                    "PDF/A (ISO 19005 standard)",
                    "Legal Document & Archival Standard",
                    "PDF/A is the mandatory international standard for electronic court submissions, legal contracts, and long-term archiving. Unlike standard editable DOCX files, PDF/A embeds all font families, color profiles, and metadata, guaranteeing immutable pagination and forensic authenticity indefinitely.",
                    Arrays.asList(
                            "Convert from DOCX to PDF to lock pagination, typography, and margins.",
                            "Never send editable Word files (.docx) for binding legal execution.",
                            "Use PDF/A-1b or PDF/A-2b compliance to ensure self-contained electronic archiving without dynamic external script dependencies."
                    ),
                    "EXPERT_KNOWLEDGE_ENGINE"
            );
        }

        // Print & Physical Media
        if (q.contains("print") || q.contains("poster") || q.contains("brochure") || q.contains("flyer") || q.contains("dpi") || q.contains("cmyk")) {
            return new AssistantResponseDto(
                    query,
                    "300 DPI PDF or TIFF",
                    "Commercial Print & Publishing Specification",
                    "For physical printing, resolution must be calculated at 300 DPI (Dots Per Inch) at physical dimensions (e.g. 2480 × 3508 px for standard A4). Use PDF or uncompressed TIFF to prevent lossy JPEG artifacting around text edges.",
                    Arrays.asList(
                            "A4 Document Print: 2480 × 3508 pixels at 300 DPI.",
                            "Photo Prints: 4×6 inch = 1200 × 1800 px, 8×10 inch = 2400 × 3000 px.",
                            "Color space: CMYK is standard for commercial litho presses; convert from RGB prior to platesetters."
                    ),
                    "EXPERT_KNOWLEDGE_ENGINE"
            );
        }

        // Web Performance & Speed
        if (q.contains("web") || q.contains("website") || q.contains("page speed") || q.contains("seo") || q.contains("fast")) {
            return new AssistantResponseDto(
                    query,
                    "WEBP",
                    "Next-Gen Web Image Optimization",
                    "Google recommends WebP for all modern web experiences. It offers 26% smaller file sizes than PNG and 25-34% smaller sizes than comparable JPEG, maintaining superior SSIM visual fidelity while supporting full alpha transparency.",
                    Arrays.asList(
                            "Hero banners: 1600-1920 px width at 80-85% compression quality.",
                            "Thumbnails / Blog cards: 600-800 px width at 75-80% quality.",
                            "Enable 'Keep Aspect Ratio' in Convertly's Image Resizer to prevent distortion."
                    ),
                    "EXPERT_KNOWLEDGE_ENGINE"
            );
        }

        // Logos, Icons & Alpha Transparency
        if (q.contains("transparent") || q.contains("logo") || q.contains("icon") || q.contains("vector") || q.contains("badge")) {
            return new AssistantResponseDto(
                    query,
                    "PNG or SVG",
                    "Vector Graphics & Lossless Transparency",
                    "For brand assets, icons, and diagrams with crisp lines or alpha transparency, use PNG or vector SVG. JPEG uses discrete cosine transforms which create muddy halos around sharp contrast edges and lacks transparency.",
                    Arrays.asList(
                            "Favicons: 32×32 and 192×192 px transparent PNG.",
                            "Navbar logos: 250-400 px width at 2x pixel density for high-DPI displays.",
                            "For purely vector shapes, keep original SVG when feasible."
                    ),
                    "EXPERT_KNOWLEDGE_ENGINE"
            );
        }

        // Video & Audio Streaming
        if (q.contains("video") || q.contains("youtube") || q.contains("reel") || q.contains("audio") || q.contains("podcast") || q.contains("mp4") || q.contains("mp3")) {
            return new AssistantResponseDto(
                    query,
                    "MP4 (H.264/AAC) for Video / MP3 (192-320 kbps) for Audio",
                    "Media Streaming & Broadcast Standard",
                    "MP4 with H.264 video codec and AAC audio stream guarantees 99.9% universal hardware playback on iOS, Android, web browsers, and smart TVs. For audio-only distribution, 192 kbps MP3 balances near-CD clarity with small payload size.",
                    Arrays.asList(
                            "YouTube / Desktop 1080p: 1920 × 1080 at 30/60 fps with H.264.",
                            "Instagram Reels / TikTok 9:16: 1080 × 1920 vertical video.",
                            "Podcast audio: 128 kbps mono or 192 kbps stereo MP3."
                    ),
                    "EXPERT_KNOWLEDGE_ENGINE"
            );
        }

        // General default
        return getFallbackResponse("For modern web images, WebP is optimal. For legal & business documents, PDF is the universal standard.");
    }

    private AssistantResponseDto getFallbackResponse(String message) {
        return new AssistantResponseDto(
                "general-advice",
                "WEBP / PDF",
                "Universal Media Guidance",
                message,
                Arrays.asList(
                        "Web images: WebP at 80-85% quality balances size and visual fidelity.",
                        "Documents: PDF locks formatting across all desktop and mobile devices.",
                        "Audio: MP3 192 kbps for universal music and speech playback.",
                        "Video: MP4 H.264 1080p for seamless cross-platform playback."
                ),
                "EXPERT_KNOWLEDGE_ENGINE"
        );
    }
}
