package com.verana.tests;

import com.verana.pages.DashboardPage;
import com.verana.pages.TrustRegistryPage;
import com.verana.pages.WalletModalPage;
import com.verana.utils.DriverManager;
import com.verana.utils.WaitUtils;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.UUID;

/**
 * Verana Ecosystem creation automation.
 *
 * Flow:
 * 1. Go to dashboard, sign in with Keplr
 * 2. Navigate to Trust Registry (/tr)
 * 3. Click "Create Ecosystem"
 * 4. Fill DID (unique each run), Aka, Language, Governance Doc URL
 */
public class EcosystemAutomationTest {

    private WebDriver driver;
    private DashboardPage dashboardPage;
    private TrustRegistryPage trustRegistryPage;
    private WalletModalPage walletModalPage;

    private String dashboardUrl;
    private String trustRegistryUrl;
    private int keplrUnlockPreopenSeconds;
    private boolean closeBrowserOnFinish;
    private int txSuccessWaitSeconds;
    private String ecosystemDidPrefix;
    private String ecosystemAkaUrl;
    private String ecosystemGovernanceLanguage;
    private String ecosystemGovernanceDocUrl;

    private String generatedDID;

    @BeforeMethod
    public void setUp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Verana Ecosystem Automation - Starting Test");
        System.out.println("=".repeat(60));

        // Load config lazily to provide clear errors on missing config
        java.util.Properties cfg = DriverManager.getConfig();
        dashboardUrl = cfg.getProperty("dashboard.url", "https://app.testnet.verana.network/dashboard");
        trustRegistryUrl = cfg.getProperty("trust.registry.url", "https://app.testnet.verana.network/tr");
        keplrUnlockPreopenSeconds = Integer.parseInt(cfg.getProperty("keplr.unlock.preopen.seconds", "4"));
        closeBrowserOnFinish = Boolean.parseBoolean(cfg.getProperty("close.browser.on.finish", "false"));
        txSuccessWaitSeconds = Integer.parseInt(cfg.getProperty("tx.success.wait.seconds", "60"));
        ecosystemDidPrefix = cfg.getProperty("ecosystem.did.prefix", "did:verana:");
        ecosystemAkaUrl = cfg.getProperty("ecosystem.aka.url", "https://app.testnet.verana.network/tr");
        ecosystemGovernanceLanguage = cfg.getProperty("ecosystem.governance.language", "English");
        ecosystemGovernanceDocUrl = cfg.getProperty("ecosystem.governance.doc.url", "https://app.testnet.verana.network/tr");

        driver = DriverManager.getDriver();
        dashboardPage = new DashboardPage(driver);
        trustRegistryPage = new TrustRegistryPage(driver);
        walletModalPage = new WalletModalPage(driver);

        // Step 1: Wait for Keplr extension to load, then unlock it
        WaitUtils.sleep(3000);
        String keplrExtId = cfg.getProperty("keplr.extension.id", "dmkamcknogkgcdfhhbddcghachkejeap").trim();
        String keplrPopupUrl = "chrome-extension://" + keplrExtId + "/popup.html";
        System.out.println("[setUp] STEP 1: Opening Keplr popup to unlock: " + keplrPopupUrl);
        driver.get(keplrPopupUrl);
        // Wait for Keplr popup to fully render (extension pages can be slow)
        WaitUtils.sleep(3000);

        // Try to unlock — use a generous timeout since Keplr may need time to load
        int unlockTimeout = Math.max(keplrUnlockPreopenSeconds, 8);
        boolean unlocked = walletModalPage.tryAutoUnlockInOpenContexts(unlockTimeout);
        if (!unlocked) {
            System.out.println("[setUp] WARNING: Keplr auto-unlock may have failed. Trying once more...");
            WaitUtils.sleep(2000);
            unlocked = walletModalPage.tryAutoUnlockInOpenContexts(unlockTimeout);
        }
        System.out.println("[setUp] STEP 1 DONE: Keplr unlock " + (unlocked ? "succeeded." : "may have failed — check logs."));

        // Step 2: Navigate to dashboard
        System.out.println("[setUp] STEP 2: Navigating to dashboard...");
        driver.get(dashboardUrl);
        WaitUtils.sleep(2000);
        System.out.println("[setUp] STEP 2 DONE: On dashboard. Moving to Trust Registry...");

        // Step 3: Go directly to Trust Registry page
        driver.get(trustRegistryUrl);
        WaitUtils.sleep(1000);
        System.out.println("[setUp] STEP 3 DONE: On Trust Registry page.");

        // Generate unique DID for this run
        generatedDID = generateUniqueEcosystemDID();
        System.out.println("  Ecosystem DID for this run: " + generatedDID);
        System.out.println("=".repeat(60) + "\n");
    }

    @Test(description = "Trust Registry: Create Ecosystem with unique DID, Aka, Language, and Governance Doc URL")
    public void testCreateEcosystem() {

        // STEP 1: Click "Create Ecosystem"
        System.out.println("[Test] STEP 1: Clicking 'Create Ecosystem' button...");
        trustRegistryPage.clickCreateEcosystem();
        System.out.println("[Test] STEP 1 DONE: Create Ecosystem form opened.");

        // STEP 2: Enter DID
        System.out.println("[Test] STEP 2: Entering DID: " + generatedDID);
        trustRegistryPage.enterDID(generatedDID);
        System.out.println("[Test] STEP 2 DONE: DID entered.");

        // STEP 3: Enter Aka
        System.out.println("[Test] STEP 3: Entering Aka: " + ecosystemAkaUrl);
        trustRegistryPage.enterAka(ecosystemAkaUrl);
        System.out.println("[Test] STEP 3 DONE: Aka entered.");

        // STEP 4: Select Primary Governance Framework Language
        System.out.println("[Test] STEP 4: Selecting governance language: " + ecosystemGovernanceLanguage);
        trustRegistryPage.selectGovernanceLanguageEnglish();
        System.out.println("[Test] STEP 4 DONE: Governance language selected.");

        // STEP 5: Enter Governance Framework Primary Document URL
        System.out.println("[Test] STEP 5: Entering Governance Doc URL: " + ecosystemGovernanceDocUrl);
        trustRegistryPage.enterGovernanceDocUrl(ecosystemGovernanceDocUrl);
        System.out.println("[Test] STEP 5 DONE: Governance Doc URL entered.");

        // STEP 6: Click Confirm button
        System.out.println("[Test] STEP 6: Clicking Confirm button...");
        trustRegistryPage.clickConfirm();
        WaitUtils.sleep(1000);
        System.out.println("[Test] STEP 6 DONE: Confirm clicked.");

        // STEP 7: Approve Keplr transaction
        System.out.println("[Test] STEP 7: Waiting for Keplr popup and clicking Approve...");
        WaitUtils.sleep(3000); // Give Keplr time to register the transaction
        boolean approved = walletModalPage.approveKeplrTransaction(15);
        Assert.assertTrue(approved, "Keplr transaction approval failed — popup may not have appeared or Approve button was not found.");
        System.out.println("[Test] STEP 7 DONE: Keplr approval succeeded.");

        // STEP 8: Wait for transaction success
        System.out.println("[Test] STEP 8: Waiting for transaction success...");
        boolean success = trustRegistryPage.waitForTransactionSuccess(txSuccessWaitSeconds);
        Assert.assertTrue(success, "Transaction success message was not detected within " + txSuccessWaitSeconds + " seconds.");
        System.out.println("[Test] STEP 8 DONE: Transaction confirmed.");

        // STEP 9: Navigate back to Trust Registry page
        System.out.println("[Test] STEP 9: Navigating back to Trust Registry page...");
        driver.get(trustRegistryUrl);
        WaitUtils.sleep(2000);
        System.out.println("[Test] STEP 9 DONE: On Trust Registry page: " + driver.getCurrentUrl());

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  TEST PASSED!");
        System.out.println("  DID: " + generatedDID);
        System.out.println("=".repeat(60) + "\n");
    }

    @AfterMethod
    public void tearDown() {
        if (closeBrowserOnFinish) {
            System.out.println("[Test] Tearing down — closing browser.");
            DriverManager.quitDriver();
        } else {
            System.out.println("[Test] Tearing down — keeping browser open (close.browser.on.finish=false).");
        }
    }

    /**
     * Generates a unique ecosystem DID like: did:verana:a1b2c3d4
     * The suffix is different every run using UUID.
     */
    private String generateUniqueEcosystemDID() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        String suffix = raw.substring(0, 8).toLowerCase();
        String did = ecosystemDidPrefix + suffix;

        System.out.println("[EcosystemTest] Generated ecosystem DID: " + did);
        return did;
    }
}
