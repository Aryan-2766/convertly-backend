package com.convertly.processor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PdfProcessorTest {

    private PdfProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PdfProcessor();
    }

    private byte[] createTestPdf(int pageCount) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage(PDRectangle.A4));
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void testValidateAndGetPageCount() throws IOException {
        byte[] pdf = createTestPdf(3);
        int pages = processor.validateAndGetPageCount(pdf);
        assertEquals(3, pages);
    }

    @Test
    void testMergePdfs() throws IOException {
        byte[] pdf1 = createTestPdf(2);
        byte[] pdf2 = createTestPdf(3);

        PdfProcessor.PdfProcessResult result = processor.mergePdfs(Arrays.asList(pdf1, pdf2));
        assertNotNull(result);
        assertEquals(5, result.pageCount());
        assertTrue(result.byteSize() > 0);
    }

    @Test
    void testSplitPdfPageRange() throws IOException {
        byte[] pdf = createTestPdf(5);
        PdfProcessor.PdfProcessResult result = processor.splitPdf(pdf, "2-4");

        assertNotNull(result);
        assertEquals(3, result.pageCount());
        assertTrue(result.byteSize() > 0);
    }

    @Test
    void testParsePageRange() {
        Set<Integer> pages = processor.parsePageRange("1-3, 5", 6);
        assertEquals(4, pages.size());
        assertTrue(pages.contains(1));
        assertTrue(pages.contains(2));
        assertTrue(pages.contains(3));
        assertTrue(pages.contains(5));
    }

    @Test
    void testRejectInvalidPageRange() {
        assertThrows(IllegalArgumentException.class, () -> processor.parsePageRange("5-2", 6));
        assertThrows(IllegalArgumentException.class, () -> processor.parsePageRange("10", 5));
    }
}
