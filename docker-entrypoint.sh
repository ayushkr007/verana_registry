#!/bin/bash
# ============================================================
# Docker Entrypoint — Verana Ecosystem Automation
# ============================================================
# This script runs automatically when the container starts.
# It does three things:
#   1. Validates the environment (password, profile)
#   2. Starts Xvfb (a virtual screen) — Chrome extensions like
#      Keplr need a display to render, even without a monitor.
#   3. Runs the Maven tests.
# ============================================================

set -e

# ---------- Check that the Keplr password is set ----------
if [ -z "$KEPLR_PASSWORD" ]; then
    echo ""
    echo "============================================================"
    echo "  ERROR: KEPLR_PASSWORD is not set!"
    echo "============================================================"
    echo ""
    echo "  Option 1 — Create a .env file (recommended):"
    echo "    cp .env.example .env"
    echo "    # Edit .env and set your password"
    echo "    docker-compose up"
    echo ""
    echo "  Option 2 — Pass it directly:"
    echo "    KEPLR_PASSWORD='YourPassword' docker-compose up"
    echo ""
    echo "  Option 3 — Add to your shell profile (permanent):"
    echo "    echo 'export KEPLR_PASSWORD=\"YourPassword\"' >> ~/.zshrc"
    echo "    source ~/.zshrc"
    echo "    docker-compose up"
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
    echo "  You need to set up your Keplr wallet first (one-time):"
    echo ""
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
# Previous container runs may leave lock files in the mounted profile
# that prevent Chrome from starting again
echo "[entrypoint] Cleaning up stale Chrome lock files..."
rm -f "$PROFILE_DIR/SingletonLock" \
      "$PROFILE_DIR/SingletonSocket" \
      "$PROFILE_DIR/SingletonCookie" \
      "$PROFILE_DIR/Profile 1/SingletonLock" \
      "$PROFILE_DIR/Profile 1/SingletonSocket" \
      "$PROFILE_DIR/Profile 1/SingletonCookie"
# Remove any DevToolsActivePort files
find "$PROFILE_DIR" -name "DevToolsActivePort" -delete 2>/dev/null || true

# ---------- Start Xvfb (virtual display) ----------
# Clean up stale lock files from previous runs
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99

echo "[entrypoint] Starting virtual display (Xvfb)..."
Xvfb :99 -screen 0 1920x1080x24 -nolisten tcp &
XVFB_PID=$!

# Give Xvfb a moment to start
sleep 1

# Verify Xvfb is running
if ! kill -0 $XVFB_PID 2>/dev/null; then
    echo "ERROR: Xvfb failed to start."
    exit 1
fi
echo "[entrypoint] Virtual display ready."

# ---------- Run the tests ----------
echo ""
echo "============================================================"
echo "  Verana Ecosystem Automation — Running Tests"
echo "============================================================"
echo ""

mvn test
TEST_EXIT_CODE=$?

# ---------- Clean up ----------
kill $XVFB_PID 2>/dev/null || true

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo ""
    echo "============================================================"
    echo "  ALL TESTS PASSED"
    echo "============================================================"
else
    echo ""
    echo "============================================================"
    echo "  TESTS FAILED (exit code: $TEST_EXIT_CODE)"
    echo "============================================================"
fi

exit $TEST_EXIT_CODE
