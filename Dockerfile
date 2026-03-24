# ============================================================
# Verana Ecosystem Automation — Docker Image
# ============================================================
# Base: Debian 12 (Bookworm) — stable, has real Chromium in apt
#
# What's inside:
#   - Chromium browser      — works on both Intel and Apple Silicon
#   - ChromeDriver          — matched to Chromium version
#   - OpenJDK 11            — Java runtime to compile & run tests
#   - Maven 3               — builds the project and runs TestNG
#   - Xvfb                  — virtual display so browser extensions
#                              (like Keplr) work without a monitor
# ============================================================

FROM debian:12-slim

# ---------- prevent interactive prompts during install ----------
ENV DEBIAN_FRONTEND=noninteractive

# ---------- 1. Install system dependencies + Chromium ----------
# Debian 12 has real Chromium in apt (not snap like Ubuntu 22.04)
# chromium         → the browser (works on ARM64 + x86)
# chromium-driver  → matching chromedriver
# xvfb             → virtual display (extensions need a screen)
# openjdk-11-jdk   → Java 11 for compiling and running the project
# maven            → build tool
# fonts-liberation → fonts so web pages render correctly
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    chromium-driver \
    xvfb \
    openjdk-17-jdk \
    maven \
    fonts-liberation \
    libnss3 \
    libatk-bridge2.0-0 \
    libgtk-3-0 \
    libgbm1 \
    procps \
    && rm -rf /var/lib/apt/lists/*

# ---------- 2. Set environment variables ----------
# DISPLAY        → tells Chromium to use the virtual display from Xvfb
# CHROME_BIN     → so our code can find the browser
# CHROMEDRIVER   → tells our code to use system chromedriver
ENV DISPLAY=:99
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMEDRIVER_PATH=/usr/bin/chromedriver

# ---------- 3. Create working directory ----------
WORKDIR /app

# ---------- 4. Copy pom.xml first and download dependencies ----------
# This is a Docker caching trick: dependencies only re-download
# when pom.xml changes, not when you edit source code.
COPY pom.xml .
RUN mvn dependency:resolve -q

# ---------- 5. Copy the rest of the project ----------
COPY src/ src/
COPY config.properties.example .

# ---------- 6. Compile the project (catches errors early) ----------
RUN mvn test-compile -q

# ---------- 7. Entry point ----------
# Starts Xvfb (virtual screen) in the background, then runs tests.
# The client just does: docker run ... and everything works.
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
