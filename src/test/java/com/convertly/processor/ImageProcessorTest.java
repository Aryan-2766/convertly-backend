package com.convertly.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageProcessorTest {

    private ImageProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ImageProcessor();
    }

    private byte[] createTestPngBytes(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.BLUE);
        g.drawString("Convertly Test", 10, 20);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    @Test
    void testDetectImageFormat() throws IOException {
        byte[] png = createTestPngBytes(100, 100);
        String detected = processor.detectImageFormat(png);
        assertEquals("png", detected);
    }

    @Test
    void testConvertPngToJpg() throws IOException {
        byte[] png = createTestPngBytes(200, 150);
        ImageProcessor.ImageProcessResult result = processor.convertFormat(png, "jpg", 0.85f);

        assertNotNull(result);
        assertEquals("jpg", result.outputFormat());
        assertEquals(200, result.width());
        assertEquals(150, result.height());
        assertTrue(result.byteSize() > 0);
    }

    @Test
    void testConvertPngToWebp() throws IOException {
        byte[] png = createTestPngBytes(160, 120);
        ImageProcessor.ImageProcessResult result = processor.convertFormat(png, "webp", 0.80f);

        assertNotNull(result);
        assertEquals("webp", result.outputFormat());
        assertTrue(result.byteSize() > 0);
    }

    @Test
    void testResizeImageWithAspectRatio() throws IOException {
        byte[] png = createTestPngBytes(400, 200); // 2:1 aspect ratio
        ImageProcessor.ImageProcessResult result = processor.resizeImage(
                png, 200, 0, true, "png", 0.9f
        );

        assertNotNull(result);
        assertEquals(200, result.width());
        assertEquals(100, result.height()); // preserved 2:1 ratio
    }

    @Test
    void testRejectInvalidSignature() {
        byte[] junk = "This is not an image at all, just plain text!".getBytes();
        assertThrows(IllegalArgumentException.class, () -> processor.detectImageFormat(junk));
    }
}
