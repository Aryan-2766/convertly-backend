package com.convertly.service;

import com.convertly.dto.VideoConversionResponseDto;
import com.convertly.entity.ConversionJob;
import com.convertly.processor.AudioVideoProcessor;
import com.convertly.repository.ConversionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Service
public class VideoConversionService {

    private static final Logger log = LoggerFactory.getLogger(VideoConversionService.class);

    private final AudioVideoProcessor processor;
    private final StorageService storageService;
    private final ConversionJobRepository jobRepository;

    public VideoConversionService(AudioVideoProcessor processor,
                                   StorageService storageService,
                                   ConversionJobRepository jobRepository) {
        this.processor = processor;
        this.storageService = storageService;
        this.jobRepository = jobRepository;
    }

    public VideoConversionResponseDto convertVideo(MultipartFile file,
                                                    String outputFormat,
                                                    String resolution) throws Exception {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "video";
        String inputFormat = detectVideoFormat(originalName);

        Path scratchDir = storageService.getScratchDirectory();
        byte[] converted = processor.convertVideo(
            file.getBytes(), inputFormat, outputFormat, resolution, scratchDir);

        String jobId = UUID.randomUUID().toString();
        String outputName = stripExtension(originalName) + "." + outputFormat;
        storageService.store(jobId, converted, outputName, "video/" + outputFormat);

        try {
            ConversionJob job = new ConversionJob();
            job.setId(jobId);
            job.setOperation("VIDEO_CONVERT");
            job.setOriginalFilename(originalName);
            job.setInputFormat(inputFormat.toUpperCase());
            job.setOutputFormat(outputFormat.toUpperCase());
            job.setInputSizeBytes(file.getSize());
            job.setOutputSizeBytes((long) converted.length);
            job.setStatus("COMPLETED");
            job.setStorageKey(jobId);
            job.setCreatedAt(Instant.now());
            job.setCompletedAt(Instant.now());
            job.setExpiresAt(Instant.now().plusSeconds(900));
            jobRepository.save(job);
        } catch (Exception e) {
            log.warn("Failed to save video conversion job to DB: {}", e.getMessage());
        }

        return new VideoConversionResponseDto(jobId, outputName, converted.length,
            file.getSize(), inputFormat.toUpperCase(), outputFormat.toUpperCase(), resolution);
    }

    public VideoConversionResponseDto extractAudio(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "video";
        String inputFormat = detectVideoFormat(originalName);

        Path scratchDir = storageService.getScratchDirectory();
        byte[] extracted = processor.extractAudioFromVideo(file.getBytes(), inputFormat, scratchDir);

        String jobId = UUID.randomUUID().toString();
        String outputName = stripExtension(originalName) + "_audio.mp3";
        storageService.store(jobId, extracted, outputName, "audio/mp3");

        try {
            ConversionJob job = new ConversionJob();
            job.setId(jobId);
            job.setOperation("EXTRACT_AUDIO");
            job.setOriginalFilename(originalName);
            job.setInputFormat(inputFormat.toUpperCase());
            job.setOutputFormat("MP3");
            job.setInputSizeBytes(file.getSize());
            job.setOutputSizeBytes((long) extracted.length);
            job.setStatus("COMPLETED");
            job.setStorageKey(jobId);
            job.setCreatedAt(Instant.now());
            job.setCompletedAt(Instant.now());
            job.setExpiresAt(Instant.now().plusSeconds(900));
            jobRepository.save(job);
        } catch (Exception e) {
            log.warn("Failed to save extract-audio job to DB: {}", e.getMessage());
        }

        return new VideoConversionResponseDto(jobId, outputName, extracted.length,
            file.getSize(), inputFormat.toUpperCase(), "MP3", null);
    }

    private String detectVideoFormat(String filename) {
        if (filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            if (AudioVideoProcessor.VIDEO_FORMATS.contains(ext)) return ext;
        }
        throw new IllegalArgumentException("Cannot detect video format for: " + filename +
            ". Supported: " + AudioVideoProcessor.VIDEO_FORMATS);
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }
}
