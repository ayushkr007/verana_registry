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

---

## Quick Start (Docker — recommended)

No need to install Java, Maven, or Chrome manually. Just **Docker** and **Chrome** (for one-time Keplr setup).

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) — install and make sure it's running
- [Google Chrome](https://www.google.com/chrome/) — only needed for the one-time Keplr wallet setup

### Step 1: Clone the project

```bash
git clone https://github.com/ayushkr007/verana_registry.git
cd verana_registry
```

### Step 2: Run the setup script (one-time)

```bash
chmod +x setup.sh
./setup.sh
```

The script will:
1. Ask for your Keplr wallet password (hidden input — no one can see it)
2. Create a `.env` file with your password (gitignored — never pushed to GitHub)
3. Open Chrome so you can install Keplr and import/create your wallet
4. Tell you the next step when done

### Step 3: Run the tests

```bash
docker-compose up --build
```

### Step 4: Watch the browser live

Open this link in your browser while the tests are running:

```
http://localhost:7900/vnc.html
```

You'll see Chrome running inside the container — Keplr unlocking, form filling, transaction signing — all in real-time.

### Step 5: View the test report

After the tests finish, open the HTML report:

```bash
open reports/emailable-report.html    # macOS
xdg-open reports/emailable-report.html  # Linux
```

Or just open `reports/emailable-report.html` in any browser.

### Re-running tests

```bash
docker-compose up           # use cached build
docker-compose up --build   # rebuild after code changes
```

---

## Using the pre-built Docker image

If you don't want to build from source, pull the pre-built image from Docker Hub:

```bash
docker pull ayushkr007/verana-automation:latest
```

Then run:

```bash
docker run -e KEPLR_PASSWORD='YourPassword' \
  -v ~/selenium-keplr-profile:/root/selenium-keplr-profile \
  -v ./reports:/app/reports \
  -p 7900:7900 \
  --shm-size=2g \
  ayushkr007/verana-automation:latest
```

Watch the browser live at: `http://localhost:7900/vnc.html`

> **Note:** You still need to complete Step 2 (Keplr wallet setup) before running.

---

## Manual Setup (without Docker)

### Prerequisites

- **Java 11+**
- **Maven 3.6+**
- **Google Chrome**

### Installing prerequisites

#### macOS

```bash
brew install openjdk@11 maven
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

### Setup

```bash
git clone https://github.com/ayushkr007/verana_registry.git
cd verana_registry
cp config.properties.example config.properties
chmod +x launch_chrome.sh
./launch_chrome.sh    # set up Keplr wallet, then close Chrome
```

Set your Keplr password:

```bash
echo 'export KEPLR_PASSWORD="YourKeplrPassword"' >> ~/.zshrc
source ~/.zshrc
```

### Run

```bash
mvn test
```

---

## Troubleshooting

### Docker: "KEPLR_PASSWORD is not set"

Run the setup script: `./setup.sh`

Or create a `.env` file manually:
```bash
cp .env.example .env
# Edit .env and set your password
```

### Docker: "Keplr Chrome profile not found"

Run the setup script: `./setup.sh`

Or manually:
```bash
chmod +x launch_chrome.sh
./launch_chrome.sh
```
Install Keplr, set up your wallet, close Chrome, then retry.

### Docker: "Cannot connect to the Docker daemon"

Docker Desktop isn't running. Open Docker Desktop and wait for it to start, then retry.

### "Failed to launch Chrome" / "user-data-dir is already in use"

Close **all** Chrome windows:

```bash
# macOS
pkill -f "Google Chrome"

# Linux
pkill chrome

# Windows (PowerShell)
Stop-Process -Name chrome -Force
```

### noVNC page is blank / not loading

Wait a few seconds — noVNC starts after Xvfb. If still blank, check that port 7900 is not used by another application.

### Keplr is locked / password not working

Make sure `KEPLR_PASSWORD` is set:
```bash
echo $KEPLR_PASSWORD
```

### Test reports not generated

Reports are saved to `./reports/` after the test run. If the directory is empty, the tests may not have reached the execution phase — check the terminal output for errors.
