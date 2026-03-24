# ============================================================
# Verana Ecosystem Automation — Docker Image
# ============================================================
# Base: Debian 12 (Bookworm) — stable, has real Chromium in apt
#
# What's inside:
#   - Chromium browser      — works on both Intel and Apple Silicon
#   - ChromeDriver          — matched to Chromium version
#   - OpenJDK 17            — Java runtime to compile & run tests
#   - Maven 3               — builds the project and runs TestNG
#   - Xvfb                  — virtual display for browser extensions
#   - noVNC + x11vnc        — watch the browser live at localhost:7900
# ============================================================

FROM debian:12-slim

# ---------- prevent interactive prompts during install ----------
ENV DEBIAN_FRONTEND=noninteractive

# ---------- 1. Install system dependencies + Chromium + noVNC ----------
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    chromium-driver \
    xvfb \
    x11vnc \
    novnc \
    python3-websockify \
    openjdk-17-jdk-headless \
    maven \
    fonts-liberation \
    libnss3 \
    libatk-bridge2.0-0 \
    libgtk-3-0 \
    libgbm1 \
    procps \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* /tmp/* /var/tmp/*

# ---------- 2. Set environment variables ----------
ENV DISPLAY=:99
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMEDRIVER_PATH=/usr/bin/chromedriver

# ---------- 3. Create working directory ----------
WORKDIR /app

# ---------- 4. Copy pom.xml first and download dependencies ----------
COPY pom.xml .
RUN mvn dependency:resolve -q

# ---------- 5. Copy the rest of the project ----------
COPY src/ src/
COPY config.properties.example .

# ---------- 6. Compile + clean up Maven cache ----------
RUN mvn test-compile -q \
    && find /root/.m2/repository -name "*-sources.jar" -delete 2>/dev/null || true \
    && find /root/.m2/repository -name "*-javadoc.jar" -delete 2>/dev/null || true

# ---------- 7. Entry point ----------
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 7900 5900

ENTRYPOINT ["/docker-entrypoint.sh"]
