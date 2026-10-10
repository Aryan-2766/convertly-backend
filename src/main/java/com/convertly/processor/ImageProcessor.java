package com.convertly.processor;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/**
 * Image Processor handling image format conversions, resizing, and quality compression.
 * Employs Java ImageIO, TwelveMonkeys WebP plugin, and Thumbnailator.
 */
@Component
public class ImageProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessor.class);

    public record ImageProcessResult(byte[] data, String outputFormat, int width, int height, long byteSize) {}

    /**
     * Inspects magic bytes / signatures to verify genuine image content.
     */
    public String detectImageFormat(byte[] data) {
        if (data == null || data.length < 12) {
            throw new IllegalArgumentException("File is empty or too small to be a valid image");
        }

        // JPEG: FF D8 FF
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "jpg";
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((data[0] & 0xFF) == 0x89 && (data[1] & 0xFF) == 0x50 && (data[2] & 0xFF) == 0x4E && (data[3] & 0xFF) == 0x47) {
            return "png";
        }

        // WebP: 'RIFF' .... 'WEBP'
        if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return "webp";
        }

        // BMP: 42 4D ('BM')
        if (data[0] == 0x42 && data[1] == 0x4D) {
            return "bmp";
        }

        // Fallback: try ImageIO reader SPI
        try {
            BufferedImage test = ImageIO.read(new ByteArrayInputStream(data));
            if (test != null) {
                return "image";
            }
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("Unsupported or invalid image file signature");
    }

    /**
     * Converts image to target format with optional quality parameter.
     */
    public ImageProcessResult convertFormat(byte[] inputBytes, String targetFormat, float quality) throws IOException {
        String normalizedTarget = normalizeFormat(targetFormat);
        detectImageFormat(inputBytes); // Validates signature

        BufferedImage original = ImageIO.read(new ByteArrayInputStream(inputBytes));
        if (original == null) {
            throw new IllegalArgumentException("Could not decode image data. Format may be corrupted.");
        }

        BufferedImage preparedImage = handleTransparency(original, normalizedTarget);
        byte[] outputBytes = writeImage(preparedImage, normalizedTarget, quality);

        return new ImageProcessResult(
                outputBytes,
                normalizedTarget,
                preparedImage.getWidth(),
                preparedImage.getHeight(),
                outputBytes.length
        );
    }

    /**
     * Resizes image to specified dimensions with aspect ratio preservation and format conversion.
     */
    public ImageProcessResult resizeImage(byte[] inputBytes, int targetWidth, int targetHeight,
                                          boolean keepAspectRatio, String targetFormat, float quality) throws IOException {
        detectImageFormat(inputBytes);

        BufferedImage original = ImageIO.read(new ByteArrayInputStream(inputBytes));
        if (original == null) {
            throw new IllegalArgumentException("Could not decode image data for resizing.");
        }

        if (targetWidth <= 0 && targetHeight <= 0) {
            throw new IllegalArgumentException("At least one dimension (width or height) must be greater than 0");
        }

        int origW = original.getWidth();
        int origH = original.getHeight();

        int finalW = targetWidth;
        int finalH = targetHeight;

        if (keepAspectRatio) {
            if (finalW > 0 && finalH <= 0) {
                finalH = (int) Math.round((double) origH * finalW / origW);
            } else if (finalH > 0 && finalW <= 0) {
                finalW = (int) Math.round((double) origW * finalH / origH);
            } else if (finalW > 0 && finalH > 0) {
                double ratio = Math.min((double) finalW / origW, (double) finalH / origH);
                finalW = (int) Math.round(origW * ratio);
                finalH = (int) Math.round(origH * ratio);
            }
        } else {
            if (finalW <= 0) finalW = origW;
            if (finalH <= 0) finalH = origH;
        }

        finalW = Math.max(1, finalW);
        finalH = Math.max(1, finalH);

        // Perform high quality scaling using Thumbnailator
        ByteArrayOutputStream resizedOs = new ByteArrayOutputStream();
        Thumbnails.of(original)
                .size(finalW, finalH)
                .keepAspectRatio(keepAspectRatio)
                .outputFormat("png") // intermediate lossless buffer
                .toOutputStream(resizedOs);

        BufferedImage resized = ImageIO.read(new ByteArrayInputStream(resizedOs.toByteArray()));

        String normalizedTarget = normalizeFormat(targetFormat != null && !targetFormat.isBlank() ? targetFormat : "jpg");
        BufferedImage preparedImage = handleTransparency(resized, normalizedTarget);
        byte[] outputBytes = writeImage(preparedImage, normalizedTarget, quality);

        return new ImageProcessResult(
                outputBytes,
                normalizedTarget,
                preparedImage.getWidth(),
                preparedImage.getHeight(),
                outputBytes.length
        );
    }

    /**
     * Handles transparent alpha channels when converting to non-alpha formats like JPEG.
     * Draws transparent pixels over a clean white background to prevent dark artifacts.
     */
    private BufferedImage handleTransparency(BufferedImage source, String targetFormat) {
        boolean targetSupportsAlpha = "png".equalsIgnoreCase(targetFormat) || "webp".equalsIgnoreCase(targetFormat);

        if (!targetSupportsAlpha && source.getColorModel().hasAlpha()) {
            BufferedImage rgbImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, source.getWidth(), source.getHeight());
                g.drawImage(source, 0, 0, null);
            } finally {
                g.dispose();
            }
            return rgbImage;
        }

        return source;
    }

    /**
     * Encodes BufferedImage to target format with specified compression quality.
     */
    private byte[] writeImage(BufferedImage image, String format, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Clamp quality between 0.1 and 1.0
        float clampedQuality = Math.max(0.1f, Math.min(1.0f, quality <= 0 ? 0.85f : quality));

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            // Fallback to basic ImageIO write
            boolean success = ImageIO.write(image, format, baos);
            if (!success) {
                throw new IOException("No suitable ImageIO writer found for format: " + format);
            }
            return baos.toByteArray();
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();

            if (param.canWriteCompressed() && ("jpg".equals(format) || "jpeg".equals(format) || "webp".equals(format))) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] compTypes = param.getCompressionTypes();
                if (compTypes != null && compTypes.length > 0) {
                    param.setCompressionType(compTypes[0]);
                }
                param.setCompressionQuality(clampedQuality);
            }

            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    private String normalizeFormat(String format) {
        if (format == null) return "jpg";
        String lower = format.trim().toLowerCase(Locale.ROOT).replace(".", "");
        if ("jpeg".equals(lower)) return "jpg";
        return lower;
    }
}
