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

"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  --user-data-dir="/Users/ayushkumar2109/selenium-keplr-profile" \
  --profile-directory="Profile 1" \
  --load-extension="/Users/ayushkumar2109/selenium-keplr-profile/Profile 1/Extensions/dmkamcknogkgcdfhhbddcghachkejeap/0.13.10_0" \
  --no-first-run \
  --no-default-browser-check \
  --disable-default-apps \
  "https://app.testnet.verana.network/dashboard"
