# Convertly Backend 🚀

The official backend engine for **Convertly** — a high-performance, cost-effective media and document conversion platform with zero permanent local storage and cloud database auditing.

---

## 🌟 Key Features

- **Zero Permanent Local Storage**: Converted files reside strictly in-memory or in an ephemeral scratch space with an automated 15-minute scheduled purge (`@Scheduled`).
- **Cloud Audit Logging**: Real-time conversion job history and metadata tracked in **Aiven Cloud MySQL**.
- **Image Processing**:
  - Format conversion: JPG, PNG, WebP, BMP (with alpha-compositing and transparency support).
  - Resizing & compression: Thumbnailator-powered with aspect-ratio preservation and custom quality presets.
- **Document & PDF Processing**:
  - Apache PDFBox 3.x integration for multi-document merging and page-range splitting (`1-3, 5`, etc.).
  - DOCX to PDF conversion with headless LibreOffice support.
- **Audio & Video Processing (FFmpeg)**:
  - Audio conversion: MP3, WAV, AAC, OGG, FLAC, M4A.
  - Video conversion: MP4, MKV, AVI, MOV, WebM.
  - Video compression & resolution presets: 720p HD, 480p SD, 360p Low.
  - Audio extraction: Strip audio track from video directly into MP3.
- **Batch Processing**:
  - Download multiple converted outputs as a bundled ZIP archive with deduplicated filenames.
- **AI-Powered Local Format Advisor**:
  - Zero-cost heuristic rule engine that advises optimal target formats and presets for specific use cases (e.g. web, print, lossless).

---

## 🛠️ Tech Stack & Prerequisites

- **Java**: JDK 17+ (JDK 25 compatible)
- **Framework**: Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Spring Validation)
- **Database**: MySQL 8.4+ (Aiven Cloud MySQL recommended)
- **Build Tool**: Apache Maven 3.9+
- **External CLI Tools**:
  - **FFmpeg**: Required for audio and video tools (`winget install Gyan.FFmpeg` or `scoop install ffmpeg`)
  - **LibreOffice**: Optional for DOCX to PDF conversion

---

## ⚙️ Configuration & Environment Variables

Copy `application.properties.example` to `application.properties` or provide environment variables:

| Variable | Description | Example |
| :--- | :--- | :--- |
| `DB_URL` | JDBC URL for MySQL | `jdbc:mysql://host:port/defaultdb?sslMode=REQUIRED` |
| `DB_USER` | MySQL Username | `avnadmin` |
| `DB_PASSWORD` | MySQL Password | `your_secret_password` |
| `PORT` | Server Port (defaults to 8080) | `8080` |

### Local Development
You can create an `application-local.properties` file in `src/main/resources/` (which is already git-ignored) to store your local credentials securely.

---

## 🚀 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/Aryan-2766/convertly-backend.git
cd convertly-backend
```

### 2. Build the application
```bash
mvn clean compile
```

### 3. Run unit & integration tests
```bash
mvn test
```

### 4. Run the application
```bash
mvn spring-boot:run
```
The server will start on `http://localhost:8080`.

---

## 📡 REST API Reference

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/health` | `GET` | Health check and Aiven MySQL connectivity status |
| `/api/tools` | `GET` | Dynamic registry of available conversion tools & formats |
| `/api/files/images/convert` | `POST` | Convert image formats (JPG, PNG, WebP, BMP) |
| `/api/files/images/resize` | `POST` | Resize and compress images |
| `/api/files/pdfs/merge` | `POST` | Merge multiple PDF documents into one |
| `/api/files/pdfs/split` | `POST` | Split PDF by page range |
| `/api/files/documents/docx-to-pdf` | `POST` | Convert DOCX documents to PDF |
| `/api/files/audio/convert` | `POST` | Convert audio formats via FFmpeg |
| `/api/files/video/convert` | `POST` | Convert video formats with resolution presets |
| `/api/files/video/extract-audio` | `POST` | Extract audio from video into MP3 |
| `/api/batch/download?jobIds=...` | `GET` | Stream multiple job results into a single ZIP archive |
| `/api/jobs/{id}/download` | `GET` | Download converted file by Job ID |
| `/api/history` | `GET` | Retrieve latest conversion audit history from Aiven MySQL |
| `/api/assistant/suggest` | `POST` | Format recommendations from the advisor engine |

---

## 🔒 Security & Privacy

- **No Shell Concatenation**: FFmpeg and LibreOffice commands are invoked via array-based process parameters, preventing shell injection vulnerabilities.
- **Path Traversal Protection**: Ephemeral storage keys and input filenames are sanitized to prevent directory traversal.
- **Automated Scratch Purging**: Files are strictly transient with a 15-minute TTL.

---

## 📄 License
MIT License. Created for College Full Stack Development Project.
