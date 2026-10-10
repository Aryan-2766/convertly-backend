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
import java.util.concurrent.TimeUnit;

/**
 * Document Processor handling Office documents (DOCX, PPTX) to PDF conversions.
 * Invokes local or container headless LibreOffice.
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

        // Check if soffice or libreoffice is in system PATH (Linux / Docker / Mac)
        String[] commands = {"soffice", "libreoffice"};
        for (String cmd : commands) {
            try {
                Process process = new ProcessBuilder(cmd, "--version").start();
                boolean finished = process.waitFor(2, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    return cmd;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /**
     * Converts DOCX bytes to PDF.
     */
    public DocumentProcessResult convertDocxToPdf(byte[] docxBytes, String originalFilename) throws IOException, InterruptedException {
        validateOfficeZipMagicBytes(docxBytes, "Microsoft Word (.docx)");
        return convertDocumentToPdf(docxBytes, "input.docx", "input.pdf");
    }

    /**
     * Converts PPTX bytes to PDF.
     */
    public DocumentProcessResult convertPptxToPdf(byte[] pptxBytes, String originalFilename) throws IOException, InterruptedException {
        validateOfficeZipMagicBytes(pptxBytes, "Microsoft PowerPoint (.pptx)");
        return convertDocumentToPdf(pptxBytes, "input.pptx", "input.pdf");
    }

    private void validateOfficeZipMagicBytes(byte[] bytes, String documentType) {
        // Modern OpenXML (DOCX, PPTX, XLSX) files are ZIP packages starting with 'PK\x03\x04'
        if (bytes == null || bytes.length < 4 ||
                bytes[0] != 0x50 || bytes[1] != 0x4B || bytes[2] != 0x03 || bytes[3] != 0x04) {
            throw new IllegalArgumentException("Invalid file format. Uploaded file is not a valid " + documentType + " document.");
        }
    }

    private DocumentProcessResult convertDocumentToPdf(byte[] inputBytes, String tempInputName, String tempOutputName) throws IOException, InterruptedException {
        String sofficePath = detectLibreOfficeExecutable();
        if (sofficePath == null) {
            throw new IllegalStateException(
                    "LibreOffice is not installed on this system. " +
                    "To enable Office-to-PDF conversion, install LibreOffice (https://www.libreoffice.org), " +
                    "or add soffice to your system PATH."
            );
        }

        Path tempDir = Files.createTempDirectory("convertly_doc_");
        Path tempFile = tempDir.resolve(tempInputName);

        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                fos.write(inputBytes);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    sofficePath,
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", tempDir.toAbsolutePath().toString(),
                    tempFile.toAbsolutePath().toString()
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(45, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Document-to-PDF conversion timed out after 45 seconds");
            }

            Path expectedPdf = tempDir.resolve(tempOutputName);
            if (!Files.exists(expectedPdf)) {
                throw new IOException("Conversion process completed, but output PDF was not generated.");
            }

            byte[] pdfBytes = Files.readAllBytes(expectedPdf);
            return new DocumentProcessResult(pdfBytes, tempOutputName, pdfBytes.length);
        } finally {
            // Clean up temporary conversion files
            try {
                Files.deleteIfExists(tempDir.resolve(tempOutputName));
                Files.deleteIfExists(tempFile);
                Files.deleteIfExists(tempDir);
            } catch (Exception ignored) {
            }
        }
    }
}
