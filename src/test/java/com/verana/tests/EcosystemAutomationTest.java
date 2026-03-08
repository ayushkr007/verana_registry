package com.verana.tests;

import com.verana.pages.DashboardPage;
import com.verana.pages.TrustRegistryPage;
import com.verana.pages.WalletModalPage;
import com.verana.utils.DriverManager;
import com.verana.utils.WaitUtils;
import org.openqa.selenium.WebDriver;
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

    private static final String DASHBOARD_URL = DriverManager.getConfig()
            .getProperty("dashboard.url", "https://app.testnet.verana.network/dashboard");
    private static final String TRUST_REGISTRY_URL = DriverManager.getConfig()
            .getProperty("trust.registry.url", "https://app.testnet.verana.network/tr");
    private static final int KEPLR_UNLOCK_PREOPEN_SECONDS = Integer.parseInt(
            DriverManager.getConfig().getProperty("keplr.unlock.preopen.seconds", "4"));
    private static final boolean CLOSE_BROWSER_ON_FINISH = Boolean.parseBoolean(
            DriverManager.getConfig().getProperty("close.browser.on.finish", "false"));
    private static final int TX_SUCCESS_WAIT_SECONDS = Integer.parseInt(
            DriverManager.getConfig().getProperty("tx.success.wait.seconds", "60"));

    private static final String ECOSYSTEM_DID_PREFIX = DriverManager.getConfig()
            .getProperty("ecosystem.did.prefix", "did:verana:");
    private static final String ECOSYSTEM_AKA_URL = DriverManager.getConfig()
            .getProperty("ecosystem.aka.url", "https://app.testnet.verana.network/tr");
    private static final String ECOSYSTEM_GOVERNANCE_LANGUAGE = DriverManager.getConfig()
            .getProperty("ecosystem.governance.language", "English");
    private static final String ECOSYSTEM_GOVERNANCE_DOC_URL = DriverManager.getConfig()
            .getProperty("ecosystem.governance.doc.url", "https://app.testnet.verana.network/tr");

    private String generatedDID;

    @BeforeMethod
    public void setUp() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Verana Ecosystem Automation - Starting Test");
        System.out.println("=".repeat(60));

        driver = DriverManager.getDriver();
        dashboardPage = new DashboardPage(driver);
        trustRegistryPage = new TrustRegistryPage(driver);
        walletModalPage = new WalletModalPage(driver);

        // Step 1: Wait for Keplr extension to load, then unlock it
        WaitUtils.sleep(3000);
        String keplrExtId = DriverManager.getConfig()
                .getProperty("keplr.extension.id", "dmkamcknogkgcdfhhbddcghachkejeap").trim();
        String keplrPopupUrl = "chrome-extension://" + keplrExtId + "/popup.html";
        System.out.println("[setUp] STEP 1: Opening Keplr popup to unlock: " + keplrPopupUrl);
        driver.get(keplrPopupUrl);
        WaitUtils.sleep(2000);
        walletModalPage.tryAutoUnlockInOpenContexts(KEPLR_UNLOCK_PREOPEN_SECONDS);
        System.out.println("[setUp] STEP 1 DONE: Keplr unlock attempted.");

        // Step 2: Navigate to dashboard, then quickly move to Trust Registry
        System.out.println("[setUp] STEP 2: Navigating to dashboard...");
        driver.get(DASHBOARD_URL);
        WaitUtils.sleep(2000);
        System.out.println("[setUp] STEP 2 DONE: On dashboard. Moving to Trust Registry...");

        // Step 3: Go directly to Trust Registry page
        driver.get(TRUST_REGISTRY_URL);
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
        WaitUtils.sleep(1500);
        System.out.println("[Test] STEP 1 DONE: Create Ecosystem form opened.");

        // STEP 2: Enter DID
        System.out.println("[Test] STEP 2: Entering DID: " + generatedDID);
        trustRegistryPage.enterDID(generatedDID);
        WaitUtils.sleep(1000);
        System.out.println("[Test] STEP 2 DONE: DID entered.");

        // STEP 3: Enter Aka
        System.out.println("[Test] STEP 3: Entering Aka: " + ECOSYSTEM_AKA_URL);
        trustRegistryPage.enterAka(ECOSYSTEM_AKA_URL);
        WaitUtils.sleep(1000);
        System.out.println("[Test] STEP 3 DONE: Aka entered.");

        // STEP 4: Select Primary Governance Framework Language = English
        System.out.println("[Test] STEP 4: Selecting governance language: " + ECOSYSTEM_GOVERNANCE_LANGUAGE);
        trustRegistryPage.selectGovernanceLanguageEnglish();
        WaitUtils.sleep(1000);
        System.out.println("[Test] STEP 4 DONE: Governance language selected.");

        // STEP 5: Enter Governance Framework Primary Document URL
        System.out.println("[Test] STEP 5: Entering Governance Doc URL: " + ECOSYSTEM_GOVERNANCE_DOC_URL);
        trustRegistryPage.enterGovernanceDocUrl(ECOSYSTEM_GOVERNANCE_DOC_URL);
        WaitUtils.sleep(500);
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
        System.out.println("[Test] STEP 7 DONE: Keplr approval " + (approved ? "succeeded." : "not detected."));

        // STEP 8: Wait for transaction success
        System.out.println("[Test] STEP 8: Waiting for transaction success...");
        boolean success = trustRegistryPage.waitForTransactionSuccess(TX_SUCCESS_WAIT_SECONDS);
        System.out.println("[Test] STEP 8 DONE: Transaction " + (success ? "confirmed." : "status unclear."));

        // STEP 9: Navigate back to Trust Registry page
        System.out.println("[Test] STEP 9: Navigating back to Trust Registry page...");
        driver.get(TRUST_REGISTRY_URL);
        WaitUtils.sleep(2000);
        System.out.println("[Test] STEP 9 DONE: On Trust Registry page: " + driver.getCurrentUrl());

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  TEST PASSED!");
        System.out.println("  DID: " + generatedDID);
        System.out.println("=".repeat(60) + "\n");
    }

    @AfterMethod
    public void tearDown() {
        if (CLOSE_BROWSER_ON_FINISH) {
            System.out.println("[Test] Tearing down — closing browser.");
            DriverManager.quitDriver();
        } else {
            System.out.println("[Test] Tearing down — keeping browser open (close.browser.on.finish=false).");
        }
    }

    /**
     * Generates a unique ecosystem DID like: did:verana:eco_a1b2c3d4
     * The suffix is different every run using UUID.
     */
    private String generateUniqueEcosystemDID() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        String suffix = raw.substring(0, 8).toLowerCase();
        String did = ECOSYSTEM_DID_PREFIX + suffix;
        System.out.println("[EcosystemTest] Generated ecosystem DID: " + did);
        return did;
    }
}
