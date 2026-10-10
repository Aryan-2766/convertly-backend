FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN mvn clean package -DskipTests -B

# Production runtime stage with FFmpeg and LibreOffice
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install FFmpeg and LibreOffice for audio, video, and document conversions
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    libreoffice \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy built jar from build stage
COPY --from=build /app/target/convertly-backend-*.jar app.jar

# Expose Render default port
EXPOSE 8080

# Environment variables
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# Run application
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
