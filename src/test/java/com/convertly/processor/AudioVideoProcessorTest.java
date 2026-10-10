package com.convertly.processor;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AudioVideoProcessorTest {

    private final AudioVideoProcessor processor = new AudioVideoProcessor();

    @Test
    void testFfmpegDetection() {
        boolean available = processor.isFfmpegAvailable();
        assertTrue(available, "FFmpeg should be detected either on PATH or in WinGet package directories");
        String path = processor.findFfmpegPath();
        assertNotNull(path, "FFmpeg path should not be null");
        assertTrue(path.toLowerCase().contains("ffmpeg"), "Path should contain ffmpeg");
    }

    @Test
    void testAudioValidationInvalidFormat() {
        assertThrows(java.lang.IllegalArgumentException.class, () -> {
            processor.convertAudio("fake".getBytes(), "invalid_fmt", "mp3", Path.of("."));
        });
    }

    @Test
    void testVideoValidationInvalidFormat() {
        assertThrows(java.lang.IllegalArgumentException.class, () -> {
            processor.convertVideo("fake".getBytes(), "invalid_fmt", "mp4", "original", Path.of("."));
        });
    }
}
