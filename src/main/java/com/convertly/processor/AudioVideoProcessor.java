package com.convertly.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Handles audio and video conversion using FFmpeg.
 * All FFmpeg arguments are validated individually — no shell string concatenation.
 */
@Component
public class AudioVideoProcessor {

    private static final Logger log = LoggerFactory.getLogger(AudioVideoProcessor.class);

    // Supported formats
    public static final Set<String> AUDIO_FORMATS = Set.of("mp3", "wav", "aac", "ogg", "flac", "m4a");
    public static final Set<String> VIDEO_FORMATS = Set.of("mp4", "mkv", "avi", "mov", "webm");

    // Timeout constants
    private static final long AUDIO_TIMEOUT_SECONDS = 120;
    private static final long VIDEO_TIMEOUT_SECONDS = 600;

    // -------------------------------------------------------------------------
    // FFmpeg detection
    // -------------------------------------------------------------------------

    /**
     * Finds the ffmpeg executable path. Checks PATH first, then known scoop/winget locations.
     */
    public String findFfmpegPath() {
        // 1. Check PATH
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean exited = p.waitFor(5, TimeUnit.SECONDS);
            if (exited && p.exitValue() == 0) {
                return "ffmpeg";
            }
        } catch (Exception ignored) {}

        // 2. Scoop shims location
        String userHome = System.getProperty("user.home");
        List<String> candidates = List.of(
            userHome + "\\scoop\\shims\\ffmpeg.exe",
            userHome + "\\scoop\\apps\\ffmpeg\\current\\bin\\ffmpeg.exe",
            "C:\\ProgramData\\scoop\\shims\\ffmpeg.exe",
            "C:\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
            "C:\\tools\\ffmpeg\\bin\\ffmpeg.exe"
        );

        for (String candidate : candidates) {
            if (Files.exists(Path.of(candidate))) {
                return candidate;
            }
        }

        // 3. WinGet AppData install (version-agnostic glob via filesystem walk)
        try {
            Path wingetBase = Path.of(userHome,
                "AppData", "Local", "Microsoft", "WinGet", "Packages");
            if (Files.exists(wingetBase)) {
                try (var stream = java.nio.file.Files.walk(wingetBase, 5)) {
                    return stream
                        .filter(p -> p.getFileName().toString().equalsIgnoreCase("ffmpeg.exe"))
                        .map(Path::toString)
                        .findFirst()
                        .orElse(null);
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    public boolean isFfmpegAvailable() {
        return findFfmpegPath() != null;
    }

    // -------------------------------------------------------------------------
    // Audio conversion
    // -------------------------------------------------------------------------

    /**
     * Converts an audio file to the specified output format.
     * @param inputBytes   raw bytes of the input audio file
     * @param inputFormat  e.g. "mp3", "wav"
     * @param outputFormat e.g. "ogg", "aac"
     * @param scratchDir   ephemeral temp directory for intermediate files
     * @return converted audio bytes
     */
    public byte[] convertAudio(byte[] inputBytes, String inputFormat, String outputFormat,
                                Path scratchDir) throws IOException, InterruptedException {
        validateAudioFormat(inputFormat);
        validateAudioFormat(outputFormat);

        String ffmpeg = requireFfmpeg();
        Path inputFile = scratchDir.resolve("audio_in_" + System.nanoTime() + "." + inputFormat);
        Path outputFile = scratchDir.resolve("audio_out_" + System.nanoTime() + "." + outputFormat);

        try {
            Files.write(inputFile, inputBytes);

            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpeg);
            cmd.add("-y");
            cmd.add("-i"); cmd.add(inputFile.toString());
            addAudioCodecArgs(cmd, outputFormat);
            cmd.add(outputFile.toString());

            runProcess(cmd, AUDIO_TIMEOUT_SECONDS, "Audio conversion");
            return Files.readAllBytes(outputFile);
        } finally {
            silentDelete(inputFile);
            silentDelete(outputFile);
        }
    }

    // -------------------------------------------------------------------------
    // Video conversion
    // -------------------------------------------------------------------------

    /**
     * Converts a video file to the specified output format with optional resolution preset.
     * @param inputBytes     raw bytes of the input video
     * @param inputFormat    e.g. "mp4", "avi"
     * @param outputFormat   e.g. "mkv", "webm"
     * @param resolution     one of: "original", "720p", "480p", "360p" (null = original)
     * @param scratchDir     ephemeral temp directory
     * @return converted video bytes
     */
    public byte[] convertVideo(byte[] inputBytes, String inputFormat, String outputFormat,
                                String resolution, Path scratchDir) throws IOException, InterruptedException {
        validateVideoFormat(inputFormat);
        validateVideoFormat(outputFormat);

        String ffmpeg = requireFfmpeg();
        Path inputFile = scratchDir.resolve("video_in_" + System.nanoTime() + "." + inputFormat);
        Path outputFile = scratchDir.resolve("video_out_" + System.nanoTime() + "." + outputFormat);

        try {
            Files.write(inputFile, inputBytes);

            List<String> cmd = new ArrayList<>();
            cmd.add(ffmpeg);
            cmd.add("-y");
            cmd.add("-i"); cmd.add(inputFile.toString());
            addVideoCodecArgs(cmd, outputFormat);
            addResolutionArgs(cmd, resolution);
            cmd.add(outputFile.toString());

            runProcess(cmd, VIDEO_TIMEOUT_SECONDS, "Video conversion");
            return Files.readAllBytes(outputFile);
        } finally {
            silentDelete(inputFile);
            silentDelete(outputFile);
        }
    }

    // -------------------------------------------------------------------------
    // Extract audio from video
    // -------------------------------------------------------------------------

    /**
     * Extracts audio track from a video file and returns it as MP3.
     * @param inputBytes  raw bytes of the video
     * @param inputFormat video format e.g. "mp4"
     * @param scratchDir  ephemeral temp directory
     * @return MP3 audio bytes
     */
    public byte[] extractAudioFromVideo(byte[] inputBytes, String inputFormat,
                                         Path scratchDir) throws IOException, InterruptedException {
        validateVideoFormat(inputFormat);

        String ffmpeg = requireFfmpeg();
        Path inputFile = scratchDir.resolve("vextract_in_" + System.nanoTime() + "." + inputFormat);
        Path outputFile = scratchDir.resolve("vextract_out_" + System.nanoTime() + ".mp3");

        try {
            Files.write(inputFile, inputBytes);

            List<String> cmd = List.of(
                ffmpeg, "-y",
                "-i", inputFile.toString(),
                "-vn",                  // drop video
                "-acodec", "libmp3lame",
                "-q:a", "2",            // VBR quality ~190 kbps
                outputFile.toString()
            );

            runProcess(cmd, AUDIO_TIMEOUT_SECONDS, "Audio extraction");
            return Files.readAllBytes(outputFile);
        } finally {
            silentDelete(inputFile);
            silentDelete(outputFile);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String requireFfmpeg() {
        String path = findFfmpegPath();
        if (path == null) {
            throw new IllegalStateException(
                "FFmpeg is not installed. Please install it via: scoop install ffmpeg");
        }
        return path;
    }

    private void validateAudioFormat(String fmt) {
        if (fmt == null || !AUDIO_FORMATS.contains(fmt.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported audio format: " + fmt +
                ". Supported: " + AUDIO_FORMATS);
        }
    }

    private void validateVideoFormat(String fmt) {
        if (fmt == null || !VIDEO_FORMATS.contains(fmt.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported video format: " + fmt +
                ". Supported: " + VIDEO_FORMATS);
        }
    }

    private void addAudioCodecArgs(List<String> cmd, String format) {
        switch (format.toLowerCase()) {
            case "mp3"  -> { cmd.add("-acodec"); cmd.add("libmp3lame"); cmd.add("-q:a"); cmd.add("2"); }
            case "wav"  -> { cmd.add("-acodec"); cmd.add("pcm_s16le"); }
            case "aac"  -> { cmd.add("-acodec"); cmd.add("aac"); cmd.add("-b:a"); cmd.add("192k"); }
            case "ogg"  -> { cmd.add("-acodec"); cmd.add("libvorbis"); cmd.add("-q:a"); cmd.add("4"); }
            case "flac" -> { cmd.add("-acodec"); cmd.add("flac"); }
            case "m4a"  -> { cmd.add("-acodec"); cmd.add("aac"); cmd.add("-b:a"); cmd.add("192k"); }
            default     -> throw new IllegalArgumentException("Unknown audio format: " + format);
        }
    }

    private void addVideoCodecArgs(List<String> cmd, String format) {
        switch (format.toLowerCase()) {
            case "mp4"  -> { cmd.add("-vcodec"); cmd.add("libx264"); cmd.add("-acodec"); cmd.add("aac"); cmd.add("-crf"); cmd.add("23"); }
            case "mkv"  -> { cmd.add("-vcodec"); cmd.add("libx264"); cmd.add("-acodec"); cmd.add("aac"); cmd.add("-crf"); cmd.add("23"); }
            case "avi"  -> { cmd.add("-vcodec"); cmd.add("mpeg4"); cmd.add("-acodec"); cmd.add("mp3"); }
            case "mov"  -> { cmd.add("-vcodec"); cmd.add("libx264"); cmd.add("-acodec"); cmd.add("aac"); cmd.add("-crf"); cmd.add("23"); }
            case "webm" -> { cmd.add("-vcodec"); cmd.add("libvpx-vp9"); cmd.add("-acodec"); cmd.add("libopus"); cmd.add("-crf"); cmd.add("30"); cmd.add("-b:v"); cmd.add("0"); }
            default     -> throw new IllegalArgumentException("Unknown video format: " + format);
        }
    }

    private void addResolutionArgs(List<String> cmd, String resolution) {
        if (resolution == null || resolution.isBlank() || resolution.equalsIgnoreCase("original")) {
            return;
        }
        String scale = switch (resolution.toLowerCase()) {
            case "720p"  -> "scale=-2:720";
            case "480p"  -> "scale=-2:480";
            case "360p"  -> "scale=-2:360";
            default      -> throw new IllegalArgumentException("Unknown resolution preset: " + resolution);
        };
        cmd.add("-vf");
        cmd.add(scale);
    }

    private void runProcess(List<String> cmd, long timeoutSeconds,
                            String opName) throws IOException, InterruptedException {
        log.info("Running {}: {}", opName, cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Drain stdout/stderr to avoid blocking
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException(opName + " timed out after " + timeoutSeconds + "s");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("{} failed (exit {}): {}", opName, exitCode, output);
            throw new IOException(opName + " failed with exit code " + exitCode +
                ". Check FFmpeg logs for details.");
        }
        log.info("{} completed successfully", opName);
    }

    private void silentDelete(Path p) {
        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
    }
}
