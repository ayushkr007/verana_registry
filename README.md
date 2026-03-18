# Verana Trust Registry Automation

Selenium Java automation for creating ecosystems on the Verana testnet Trust Registry.

## What it does

1. Launches Chrome with the Keplr wallet extension
2. Auto-unlocks Keplr with saved password
3. Navigates to the Verana dashboard, then to the Trust Registry page (`/tr`)
4. Clicks **Create Ecosystem** and fills the form:
   - **DID** — unique `did:verana:<random>` generated each run
   - **Aka** — `https://app.testnet.verana.network/tr`
   - **Primary Governance Framework Language** — English
   - **Governance Framework Primary Document URL** — `https://app.testnet.verana.network/tr`
5. Clicks **Confirm**, approves the Keplr transaction
6. Waits for transaction success, then navigates back to `/tr`

## Prerequisites

- **Java 11+**
- **Maven 3.6+**
- **Google Chrome**
- **A Keplr wallet** — Keplr is a browser extension wallet for Cosmos-based blockchains. If you don't have one yet, you'll create it during setup (step 3 below).

### Installing prerequisites

#### macOS

```bash
# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java 11 and Maven
brew install openjdk@11 maven

# Add Java to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### Ubuntu / Debian

```bash
sudo apt update
sudo apt install openjdk-11-jdk maven google-chrome-stable
```

#### Windows

1. Download and install [JDK 11](https://adoptium.net/)
2. Download and install [Maven](https://maven.apache.org/download.cgi) and add it to your `PATH`
3. Download and install [Google Chrome](https://www.google.com/chrome/)

### Verify installation

```bash
java -version    # should show 11+
mvn -version     # should show 3.6+
```

## Setup

### 1. Clone the repository

```bash
git clone <repository-url>
cd verana_automation
```

### 2. Create your config file

The config file is not included in the repo (it's gitignored to protect passwords and local paths). You need to create it from the provided template:

```bash
cp config.properties.example config.properties
```

The defaults work out of the box — Chrome binary and Keplr extension paths are auto-detected. You only need to edit `config.properties` if you have a non-standard Chrome installation.

### 3. Set up Keplr wallet (one-time)

This step launches Chrome with a special profile so the Keplr extension is installed and your wallet is saved for future test runs.

**macOS / Linux:**

1. Close **all** Chrome windows first (required — Chrome can only have one instance per profile)
2. Run the setup script:
   ```bash
   chmod +x launch_chrome.sh
   ./launch_chrome.sh
   ```
3. Chrome will open with the Keplr extension. In the Keplr popup:
   - If you **already have a wallet**: Click **"Import existing wallet"** → enter your 12/24-word seed phrase → set a password
   - If you **don't have a wallet**: Click **"Create a new wallet"** → save your seed phrase somewhere safe → set a password
4. Once Keplr shows your wallet is ready, **close Chrome**

**Windows:**

1. Close all Chrome windows
2. Open PowerShell and run:
   ```powershell
   & "C:\Program Files\Google\Chrome\Application\chrome.exe" `
     --user-data-dir="$env:USERPROFILE\selenium-keplr-profile" `
     --profile-directory="Profile 1" `
     --no-first-run `
     --no-default-browser-check `
     "https://app.testnet.verana.network/dashboard"
   ```
3. Install the Keplr extension from the Chrome Web Store, then import/create your wallet
4. Close Chrome

### 4. Set your Keplr password

Set the `KEPLR_PASSWORD` environment variable with the wallet password you created in step 3.

**macOS / Linux** — add this to your `~/.zshrc` or `~/.bashrc` so it persists across terminal sessions:

```bash
echo 'export KEPLR_PASSWORD="YourKeplrPassword"' >> ~/.zshrc
source ~/.zshrc
```

**Windows (PowerShell)** — set it permanently for your user:

```powershell
[System.Environment]::SetEnvironmentVariable("KEPLR_PASSWORD", "YourKeplrPassword", "User")
```

Then restart your terminal.

> **Note:** The password is never stored in any project file — it stays in your shell environment only.

## Run

```bash
mvn test
```

## Troubleshooting

### "Failed to launch Chrome" / "user-data-dir is already in use"

Chrome can only run one instance per profile. Close **all** Chrome windows and try again:

```bash
# macOS — force quit all Chrome processes
pkill -f "Google Chrome"

# Linux
pkill chrome

# Windows (PowerShell)
Stop-Process -Name chrome -Force
```

### "Keplr extension not found"

The Keplr extension hasn't been installed in the selenium profile yet. Run the setup script (`./launch_chrome.sh`) and install Keplr — see step 3 above.

### "Failed to load config.properties"

You need to create the config file from the template:
```bash
cp config.properties.example config.properties
```

### Keplr is locked / password not working

Make sure the `KEPLR_PASSWORD` environment variable is set in your current terminal session:

```bash
echo $KEPLR_PASSWORD    # should print your password
```

If it's empty, set it:

```bash
export KEPLR_PASSWORD="YourKeplrPassword"
```

To make it permanent, add the export line to your `~/.zshrc` or `~/.bashrc` (see step 4).

### Test passes but transaction status is "unclear"

The blockchain transaction may take longer than the default 60-second timeout. Increase the timeout in `config.properties`:

```properties
tx.success.wait.seconds=120
```
