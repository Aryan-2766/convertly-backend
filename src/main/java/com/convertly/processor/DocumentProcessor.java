package com.convertly.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Document Processor handling DOCX-to-PDF conversions.
 * Invokes local headless LibreOffice if available, or presents clear installation instructions.
 */
@Component
public class DocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class);

    @Value("${convertly.libreoffice.path:}")
    private String configuredLibreOfficePath;

    public record DocumentProcessResult(byte[] pdfData, String filename, long byteSize) {}

    /**
     * Detects whether LibreOffice headless is installed and returns its executable path.
     */
    public String detectLibreOfficeExecutable() {
        if (configuredLibreOfficePath != null && !configuredLibreOfficePath.isBlank()) {
            File f = new File(configuredLibreOfficePath);
            if (f.exists() && f.canExecute()) return f.getAbsolutePath();
        }

        // Standard Windows installation paths
        String[] possiblePaths = {
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"
        };

        for (String p : possiblePaths) {
            File f = new File(p);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
        }

        // Check if soffice is in system PATH
        try {
            Process process = new ProcessBuilder("soffice", "--version").start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return "soffice";
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Converts DOCX bytes to PDF.
     */
    public DocumentProcessResult convertDocxToPdf(byte[] docxBytes, String originalFilename) throws IOException, InterruptedException {
        // Validate magic bytes: DOCX is a ZIP container starting with 'PK\x03\x04'
        if (docxBytes == null || docxBytes.length < 4 ||
                docxBytes[0] != 0x50 || docxBytes[1] != 0x4B || docxBytes[2] != 0x03 || docxBytes[3] != 0x04) {
            throw new IllegalArgumentException("Invalid file format. Uploaded file is not a valid Microsoft Word (.docx) document.");
        }

        String sofficePath = detectLibreOfficeExecutable();
        if (sofficePath == null) {
            throw new IllegalStateException(
                    "LibreOffice is not installed on this system. " +
                    "To enable DOCX-to-PDF conversion, download and install LibreOffice (https://www.libreoffice.org), " +
                    "or add soffice to your system PATH."
            );
        }

        Path tempDir = Files.createTempDirectory("convertly_docx_");
        Path tempDocx = tempDir.resolve("input.docx");

        try {
            try (FileOutputStream fos = new FileOutputStream(tempDocx.toFile())) {
                fos.write(docxBytes);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    sofficePath,
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", tempDir.toAbsolutePath().toString(),
                    tempDocx.toAbsolutePath().toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("DOCX-to-PDF conversion timed out after 30 seconds");
            }

            Path expectedPdf = tempDir.resolve("input.pdf");
            if (!Files.exists(expectedPdf)) {
                throw new IOException("Conversion process completed, but output PDF was not generated.");
            }

            byte[] pdfBytes = Files.readAllBytes(expectedPdf);
            return new DocumentProcessResult(pdfBytes, "input.pdf", pdfBytes.length);
        } finally {
            // Clean up temporary conversion files
            try {
                Files.deleteIfExists(tempDir.resolve("input.pdf"));
                Files.deleteIfExists(tempDocx);
                Files.deleteIfExists(tempDir);
            } catch (Exception ignored) {
            }
        }
    }
}
