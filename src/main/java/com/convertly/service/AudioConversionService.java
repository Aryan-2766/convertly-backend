package com.convertly.service;

import com.convertly.dto.AudioConversionResponseDto;
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
public class AudioConversionService {

    private static final Logger log = LoggerFactory.getLogger(AudioConversionService.class);

    private final AudioVideoProcessor processor;
    private final StorageService storageService;
    private final ConversionJobRepository jobRepository;

    public AudioConversionService(AudioVideoProcessor processor,
                                   StorageService storageService,
                                   ConversionJobRepository jobRepository) {
        this.processor = processor;
        this.storageService = storageService;
        this.jobRepository = jobRepository;
    }

    public AudioConversionResponseDto convertAudio(MultipartFile file,
                                                    String outputFormat) throws Exception {
        String originalName = file.getOriginalFilename() != null
            ? file.getOriginalFilename() : "audio";
        String inputFormat = detectAudioFormat(originalName, file.getBytes());

        Path scratchDir = storageService.getScratchDirectory();
        byte[] converted = processor.convertAudio(file.getBytes(), inputFormat, outputFormat, scratchDir);

        String jobId = UUID.randomUUID().toString();
        String outputName = stripExtension(originalName) + "." + outputFormat;
        storageService.store(jobId, converted, outputName, "audio/" + outputFormat);

        // Save history to Aiven MySQL
        try {
            ConversionJob job = new ConversionJob();
            job.setId(jobId);
            job.setOperation("AUDIO_CONVERT");
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
            log.warn("Failed to save audio conversion job to DB: {}", e.getMessage());
        }

        return new AudioConversionResponseDto(jobId, outputName, converted.length,
            file.getSize(), inputFormat.toUpperCase(), outputFormat.toUpperCase());
    }

    private String detectAudioFormat(String filename, byte[] bytes) {
        // Magic byte detection
        if (bytes.length >= 4) {
            // MP3: ID3 tag
            if (bytes[0] == 0x49 && bytes[1] == 0x44 && bytes[2] == 0x33) return "mp3";
            // MP3: MPEG sync
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xE0) == 0xE0) return "mp3";
            // WAV: RIFF....WAVE
            if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46) return "wav";
            // OGG
            if (bytes[0] == 0x4F && bytes[1] == 0x67 && bytes[2] == 0x67) return "ogg";
            // FLAC
            if (bytes[0] == 0x66 && bytes[1] == 0x4C && bytes[2] == 0x61) return "flac";
            // M4A/AAC: ftyp box at offset 4
            if (bytes.length >= 8 && bytes[4] == 0x66 && bytes[5] == 0x74 && bytes[6] == 0x79 && bytes[7] == 0x70) return "m4a";
        }
        // Fall back to extension
        if (filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            if (AudioVideoProcessor.AUDIO_FORMATS.contains(ext)) return ext;
        }
        throw new IllegalArgumentException("Cannot detect audio format for: " + filename);
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }
}
