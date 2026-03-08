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

- Java 11+
- Maven
- Google Chrome
- Keplr wallet extension set up in the selenium Chrome profile (run `./launch_chrome.sh` once to set up)

## Setup (one-time)

1. Close all Chrome windows
2. Run `./launch_chrome.sh`
3. Import your wallet into Keplr and set the password
4. Close Chrome

## Run

```bash
mvn test
```
