#!/bin/bash
# ============================================================
# Verana Ecosystem Automation — One-Time Setup
# ============================================================
# This script does everything needed before running the tests:
#   1. Asks for your Keplr wallet password
#   2. Creates the .env file
#   3. Launches Chrome so you can set up your Keplr wallet
#
# After this script, just run: docker-compose up --build
# ============================================================

echo ""
echo "============================================================"
echo "  Verana Ecosystem Automation — Setup"
echo "============================================================"
echo ""

# ---------- Step 1: Keplr password ----------
if [ -f ".env" ]; then
    echo "  .env file already exists."
    read -p "  Do you want to overwrite it? (y/N): " OVERWRITE
    if [ "$OVERWRITE" != "y" ] && [ "$OVERWRITE" != "Y" ]; then
        echo "  Keeping existing .env file."
        echo ""
    else
        rm -f .env
    fi
fi

if [ ! -f ".env" ]; then
    echo "  Enter your Keplr wallet password."
    echo "  (This is the password you use to unlock Keplr in Chrome)"
    echo ""
    while true; do
        read -s -p "  Keplr password: " KEPLR_PASSWORD
        echo ""
        if [ -z "$KEPLR_PASSWORD" ]; then
            echo "  Password cannot be empty. Try again."
        else
            break
        fi
    done

    echo "KEPLR_PASSWORD=$KEPLR_PASSWORD" > .env
    echo ""
    echo "  .env file created. Your password is saved locally"
    echo "  and will never be pushed to GitHub."
    echo ""
fi

# ---------- Step 2: Keplr wallet setup ----------
USER_DATA_DIR="$HOME/selenium-keplr-profile"
PROFILE_DIR="Profile 1"
KEPLR_EXT_DIR="$USER_DATA_DIR/$PROFILE_DIR/Extensions/dmkamcknogkgcdfhhbddcghachkejeap"

if [ -d "$KEPLR_EXT_DIR" ]; then
    echo "  Keplr wallet profile already exists."
    read -p "  Do you want to set up Keplr again? (y/N): " SETUP_AGAIN
    if [ "$SETUP_AGAIN" != "y" ] && [ "$SETUP_AGAIN" != "Y" ]; then
        echo ""
        echo "============================================================"
        echo "  Setup complete! Run:"
        echo "    docker-compose up --build"
        echo ""
        echo "  Watch the browser live at:"
        echo "    http://localhost:7900/vnc.html"
        echo "============================================================"
        echo ""
        exit 0
    fi
fi

# Auto-detect Chrome binary
OS="$(uname -s)"
case "$OS" in
  Darwin)
    CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    ;;
  Linux)
    CHROME="$(command -v google-chrome || command -v google-chrome-stable || command -v chromium-browser || echo "google-chrome")"
    ;;
  *)
    echo "  Unsupported OS: $OS"
    echo "  Please set up Keplr manually — see README.md"
    exit 1
    ;;
esac

if [ ! -f "$CHROME" ] && [ "$OS" = "Darwin" ]; then
    echo ""
    echo "  ERROR: Google Chrome not found."
    echo "  Please install Chrome from: https://www.google.com/chrome/"
    exit 1
fi

# Auto-detect Keplr extension if already installed
if [ -d "$KEPLR_EXT_DIR" ]; then
    KEPLR_VERSION=$(ls "$KEPLR_EXT_DIR" | sort -V | tail -1)
    EXT_FLAG="--load-extension=$KEPLR_EXT_DIR/$KEPLR_VERSION"
    echo "  Loading existing Keplr extension."
else
    EXT_FLAG=""
fi

echo ""
echo "============================================================"
echo "  Chrome is opening now."
echo ""
echo "  What to do:"
echo "    1. Install the Keplr extension (if not already installed)"
echo "    2. Import or create your Keplr wallet"
echo "    3. Set a password (use the same one you entered above)"
echo "    4. Close Chrome when you're done"
echo "============================================================"
echo ""

# Launch Chrome
"$CHROME" \
  --user-data-dir="$USER_DATA_DIR" \
  --profile-directory="$PROFILE_DIR" \
  $EXT_FLAG \
  --no-first-run \
  --no-default-browser-check \
  --disable-default-apps \
  "https://app.testnet.verana.network/dashboard" 2>/dev/null

# Chrome has closed
echo ""
echo "============================================================"
echo "  Setup complete! Run:"
echo "    docker-compose up --build"
echo ""
echo "  Watch the browser live at:"
echo "    http://localhost:7900/vnc.html"
echo ""
echo "  Test reports will be saved to:"
echo "    ./reports/emailable-report.html"
echo "============================================================"
echo ""
