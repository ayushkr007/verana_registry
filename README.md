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

No need to install Java, Maven, or Chrome. Just **Docker** and **Chrome** (for one-time Keplr setup).

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) — install and make sure it's running
- [Google Chrome](https://www.google.com/chrome/) — only needed for the one-time Keplr wallet setup

### Step 1: Clone the project

```bash
git clone https://github.com/ayushkr007/verana_registry.git
cd verana_registry
```

### Step 2: Set up Keplr wallet (one-time)

Close **all** Chrome windows first, then:

```bash
chmod +x launch_chrome.sh
./launch_chrome.sh
```

Chrome opens with the Keplr extension. In the Keplr popup:
- If you **already have a wallet**: Click **"Import existing wallet"** → enter your seed phrase → set a password
- If you **don't have a wallet**: Click **"Create a new wallet"** → save your seed phrase → set a password

Once your wallet is ready, **close Chrome**.

### Step 3: Set your Keplr password

```bash
cp .env.example .env
```

Open `.env` in any text editor and replace `YourKeplrPassword` with the password you set in Step 2:

```
KEPLR_PASSWORD=YourActualPassword
```

This file is gitignored — your password stays on your machine only.

### Step 4: Run the tests

```bash
docker-compose up --build
```

You'll see `ALL TESTS PASSED` when everything works.

### Re-running tests

After the first build, just run:

```bash
docker-compose up
```

Add `--build` only if you changed the source code.

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
  --shm-size=2g \
  ayushkr007/verana-automation:latest
```

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

### Setup

#### 1. Clone the repository

```bash
git clone https://github.com/ayushkr007/verana_registry.git
cd verana_registry
```

#### 2. Create your config file

```bash
cp config.properties.example config.properties
```

#### 3. Set up Keplr wallet (one-time)

Close **all** Chrome windows first, then:

```bash
chmod +x launch_chrome.sh
./launch_chrome.sh
```

Install Keplr, import/create your wallet, set a password, then close Chrome.

**Windows:** See the Docker quick start section above for PowerShell commands.

#### 4. Set your Keplr password

**macOS / Linux:**

```bash
echo 'export KEPLR_PASSWORD="YourKeplrPassword"' >> ~/.zshrc
source ~/.zshrc
```

**Windows (PowerShell):**

```powershell
[System.Environment]::SetEnvironmentVariable("KEPLR_PASSWORD", "YourKeplrPassword", "User")
```

Then restart your terminal.

### Run

```bash
mvn test
```

---

## Troubleshooting

### Docker: "KEPLR_PASSWORD is not set"

Create a `.env` file from the template:
```bash
cp .env.example .env
```
Edit `.env` and set your password. Then run `docker-compose up` again.

### Docker: "Keplr Chrome profile not found"

You need to run the Keplr wallet setup first:
```bash
chmod +x launch_chrome.sh
./launch_chrome.sh
```
Install Keplr, set up your wallet, close Chrome, then retry.

### Docker: "Cannot connect to the Docker daemon"

Docker Desktop isn't running. Open Docker Desktop and wait for it to start, then retry.

### "Failed to launch Chrome" / "user-data-dir is already in use"

Chrome can only run one instance per profile. Close **all** Chrome windows and try again:

```bash
# macOS
pkill -f "Google Chrome"

# Linux
pkill chrome

# Windows (PowerShell)
Stop-Process -Name chrome -Force
```

### "Keplr extension not found"

The Keplr extension hasn't been installed in the selenium profile yet. Run `./launch_chrome.sh` and install Keplr.

### Keplr is locked / password not working

Make sure `KEPLR_PASSWORD` is set:

```bash
echo $KEPLR_PASSWORD    # should print your password
```

### Test passes but transaction status is "unclear"

Increase the timeout in `config.properties`:

```properties
tx.success.wait.seconds=120
```
