#!/bin/bash
# Launch Chrome with the selenium-keplr-profile (Profile 1) + Keplr extension loaded.
# Use this to manually set up Keplr (import wallet) ONCE before running tests.
#
# Steps:
# 1. Close ALL Chrome windows first
# 2. Run this script: ./launch_chrome.sh
# 3. Keplr will open — click "Import existing wallet" and set up your wallet
# 4. Once done, close Chrome
# 5. Run: mvn test

USER_DATA_DIR="$HOME/selenium-keplr-profile"
PROFILE_DIR="Profile 1"
KEPLR_EXT_DIR="$USER_DATA_DIR/$PROFILE_DIR/Extensions/dmkamcknogkgcdfhhbddcghachkejeap"

# Auto-detect Chrome binary per OS
OS="$(uname -s)"
case "$OS" in
  Darwin)
    CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    ;;
  Linux)
    CHROME="$(command -v google-chrome || command -v google-chrome-stable || command -v chromium-browser || echo "google-chrome")"
    ;;
  *)
    echo "Unsupported OS: $OS. Please run Chrome manually."
    exit 1
    ;;
esac

if [ ! -f "$CHROME" ] && [ "$OS" = "Darwin" ]; then
  echo "ERROR: Chrome not found at $CHROME"
  exit 1
fi

# Auto-detect latest Keplr version
if [ -d "$KEPLR_EXT_DIR" ]; then
  KEPLR_VERSION=$(ls "$KEPLR_EXT_DIR" | sort -V | tail -1)
  EXT_FLAG="--load-extension=$KEPLR_EXT_DIR/$KEPLR_VERSION"
  echo "Loading Keplr extension: $KEPLR_EXT_DIR/$KEPLR_VERSION"
else
  EXT_FLAG=""
  echo "WARNING: Keplr extension not found at $KEPLR_EXT_DIR"
  echo "The extension will be installed when you set up Keplr in Chrome."
fi

"$CHROME" \
  --user-data-dir="$USER_DATA_DIR" \
  --profile-directory="$PROFILE_DIR" \
  $EXT_FLAG \
  --no-first-run \
  --no-default-browser-check \
  --disable-default-apps \
  "https://app.testnet.verana.network/dashboard"
