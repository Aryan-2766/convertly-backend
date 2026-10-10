package com.convertly.processor;

import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * PDF Processor handling merging and page-range splitting using Apache PDFBox 3.x.
 */
@Component
public class PdfProcessor {

    private static final Logger log = LoggerFactory.getLogger(PdfProcessor.class);

    public record PdfProcessResult(byte[] data, int pageCount, long byteSize) {}

    /**
     * Validates that the input bytes represent a valid PDF.
     */
    public int validateAndGetPageCount(byte[] data) throws IOException {
        if (data == null || data.length < 4) {
            throw new IllegalArgumentException("File is empty or too small to be a PDF");
        }

        // Magic bytes check for %PDF
        if (data[0] != '%' || data[1] != 'P' || data[2] != 'D' || data[3] != 'F') {
            throw new IllegalArgumentException("Invalid file format. Uploaded file is not a valid PDF document.");
        }

        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(data))) {
            if (doc.isEncrypted()) {
                throw new IllegalArgumentException("Encrypted / password-protected PDFs are not supported.");
            }
            return doc.getNumberOfPages();
        }
    }

    /**
     * Merges a list of PDF byte arrays into a single combined PDF.
     */
    public PdfProcessResult mergePdfs(List<byte[]> pdfByteList) throws IOException {
        if (pdfByteList == null || pdfByteList.size() < 2) {
            throw new IllegalArgumentException("At least 2 PDF files are required for merging.");
        }

        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        merger.setDestinationStream(outputStream);

        int totalPages = 0;
        for (byte[] pdfBytes : pdfByteList) {
            totalPages += validateAndGetPageCount(pdfBytes);
            merger.addSource(new RandomAccessReadBuffer(pdfBytes));
        }

        merger.mergeDocuments(null);
        byte[] mergedBytes = outputStream.toByteArray();

        log.info("Successfully merged {} PDFs into a {}-page document ({} bytes)",
                pdfByteList.size(), totalPages, mergedBytes.length);

        return new PdfProcessResult(mergedBytes, totalPages, mergedBytes.length);
    }

    /**
     * Splits a PDF by extracting the specified page ranges (e.g. "1-3", "2,4,6", "1-2, 5").
     * Pages are 1-indexed.
     */
    public PdfProcessResult splitPdf(byte[] inputBytes, String pageRangeExpression) throws IOException {
        validateAndGetPageCount(inputBytes);

        try (PDDocument sourceDoc = Loader.loadPDF(new RandomAccessReadBuffer(inputBytes))) {
            int totalPages = sourceDoc.getNumberOfPages();
            Set<Integer> targetPages = parsePageRange(pageRangeExpression, totalPages);

            if (targetPages.isEmpty()) {
                throw new IllegalArgumentException("Page range did not match any pages in document (Document has " + totalPages + " pages).");
            }

            try (PDDocument outputDoc = new PDDocument()) {
                for (int pageNum : targetPages) {
                    // PDDocument pages are 0-indexed internally
                    outputDoc.addPage(sourceDoc.getPage(pageNum - 1));
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                outputDoc.save(baos);
                byte[] splitBytes = baos.toByteArray();

                log.info("Extracted {} pages from original {}-page PDF", targetPages.size(), totalPages);
                return new PdfProcessResult(splitBytes, targetPages.size(), splitBytes.length);
            }
        }
    }

    /**
     * Parses page range expressions like "1-3", "1, 3, 5-7", "2".
     * Returns an ordered set of 1-indexed page numbers.
     */
    public Set<Integer> parsePageRange(String expression, int totalPages) {
        Set<Integer> pages = new TreeSet<>();
        if (expression == null || expression.isBlank() || "all".equalsIgnoreCase(expression.trim())) {
            for (int i = 1; i <= totalPages; i++) pages.add(i);
            return pages;
        }

        String[] parts = expression.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.contains("-")) {
                String[] range = trimmed.split("-");
                if (range.length != 2) {
                    throw new IllegalArgumentException("Invalid range syntax: '" + trimmed + "'. Expected format e.g. 1-5");
                }
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());

                if (start > end) {
                    throw new IllegalArgumentException("Invalid page range: start page (" + start + ") is greater than end page (" + end + ").");
                }
                if (start < 1) {
                    throw new IllegalArgumentException("Page numbers must be 1 or greater.");
                }
                if (end > totalPages) {
                    throw new IllegalArgumentException("End page (" + end + ") exceeds total document pages (" + totalPages + ").");
                }

                for (int p = start; p <= end; p++) {
                    pages.add(p);
                }
            } else {
                int page = Integer.parseInt(trimmed);
                if (page < 1 || page > totalPages) {
                    throw new IllegalArgumentException("Page " + page + " is out of bounds (Document has " + totalPages + " pages).");
                }
                pages.add(page);
            }
        }

        return pages;
    }
}
