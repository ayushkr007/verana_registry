#!/bin/bash
# ============================================================
# Docker Entrypoint — Verana Ecosystem Automation
# ============================================================
# This script runs automatically when the container starts.
#   1. Validates the environment (password, profile)
#   2. Starts Xvfb (virtual screen)
#   3. Starts noVNC (so you can watch the browser live)
#   4. Runs the Maven tests
#   5. Copies test reports to ./reports/
# ============================================================

set -e

# ---------- Check that the Keplr password is set ----------
if [ -z "$KEPLR_PASSWORD" ]; then
    echo ""
    echo "============================================================"
    echo "  ERROR: KEPLR_PASSWORD is not set!"
    echo "============================================================"
    echo ""
    echo "  Option 1 — Run the setup script (recommended):"
    echo "    ./setup.sh"
    echo ""
    echo "  Option 2 — Create a .env file:"
    echo "    cp .env.example .env"
    echo "    # Edit .env and set your password"
    echo "    docker-compose up"
    echo ""
    echo "  Option 3 — Pass it directly:"
    echo "    KEPLR_PASSWORD='YourPassword' docker-compose up"
    echo ""
    exit 1
fi

# ---------- Check that Keplr Chrome profile exists ----------
PROFILE_DIR="/root/selenium-keplr-profile"
if [ ! -d "$PROFILE_DIR" ]; then
    echo ""
    echo "============================================================"
    echo "  ERROR: Keplr Chrome profile not found!"
    echo "============================================================"
    echo ""
    echo "  Run the setup script first:"
    echo "    ./setup.sh"
    echo ""
    echo "  Or manually:"
    echo "    1. Close all Chrome windows"
    echo "    2. Run: chmod +x launch_chrome.sh && ./launch_chrome.sh"
    echo "    3. Install Keplr extension in Chrome"
    echo "    4. Import/create your wallet and set a password"
    echo "    5. Close Chrome"
    echo "    6. Run: docker-compose up"
    echo ""
    exit 1
fi

# ---------- Check that config.properties exists ----------
if [ ! -f /app/config.properties ]; then
    echo "[entrypoint] config.properties not found — copying from template..."
    cp /app/config.properties.example /app/config.properties
fi

# ---------- Clean up stale Chrome lock files ----------
echo "[entrypoint] Cleaning up stale Chrome lock files..."
rm -f "$PROFILE_DIR/SingletonLock" \
      "$PROFILE_DIR/SingletonSocket" \
      "$PROFILE_DIR/SingletonCookie" \
      "$PROFILE_DIR/Profile 1/SingletonLock" \
      "$PROFILE_DIR/Profile 1/SingletonSocket" \
      "$PROFILE_DIR/Profile 1/SingletonCookie"
find "$PROFILE_DIR" -name "DevToolsActivePort" -delete 2>/dev/null || true

# ---------- Start Xvfb (virtual display) ----------
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99

echo "[entrypoint] Starting virtual display (Xvfb)..."
Xvfb :99 -screen 0 1920x1080x24 -nolisten tcp &
XVFB_PID=$!
sleep 1

if ! kill -0 $XVFB_PID 2>/dev/null; then
    echo "ERROR: Xvfb failed to start."
    exit 1
fi
echo "[entrypoint] Virtual display ready."

# ---------- Start VNC + noVNC (live browser viewing) ----------
echo "[entrypoint] Starting VNC server..."
x11vnc -display :99 -forever -nopw -shared -rfbport 5900 -q &
sleep 1

echo "[entrypoint] Starting noVNC web viewer..."
websockify --web /usr/share/novnc/ 7900 localhost:5900 > /dev/null 2>&1 &
sleep 1

echo ""
echo "============================================================"
echo "  LIVE BROWSER VIEW: http://localhost:7900/vnc.html"
echo "============================================================"
echo "  Open the link above in your browser to watch the"
echo "  automation running in real-time."
echo "============================================================"
echo ""

# ---------- Run the tests ----------
echo ""
echo "============================================================"
echo "  Verana Ecosystem Automation — Running Tests"
echo "============================================================"
echo ""

mvn test -o
TEST_EXIT_CODE=$?

# ---------- Copy test reports ----------
mkdir -p /app/reports
cp -r /app/target/surefire-reports/* /app/reports/ 2>/dev/null || true
chmod -R 777 /app/reports 2>/dev/null || true

# ---------- Clean up ----------
kill $XVFB_PID 2>/dev/null || true

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo ""
    echo "============================================================"
    echo "  ALL TESTS PASSED"
    echo "============================================================"
    echo "  HTML Report: ./reports/emailable-report.html"
    echo "============================================================"
else
    echo ""
    echo "============================================================"
    echo "  TESTS FAILED (exit code: $TEST_EXIT_CODE)"
    echo "============================================================"
    echo "  HTML Report: ./reports/emailable-report.html"
    echo "  Check the report for details."
    echo "============================================================"
fi

exit $TEST_EXIT_CODE
