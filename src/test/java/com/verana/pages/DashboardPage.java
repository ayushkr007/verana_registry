package com.verana.pages;

import com.verana.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * DashboardPage
 *
 * Represents the Verana Network Dashboard page.
 * Handles wallet connection status checks and initiating the connect flow.
 */
public class DashboardPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    // ---- Locators ----

    private final By connectButtonHeader = By.xpath(
            "//button[normalize-space()='Connect' or .//span[normalize-space()='Connect']]");
    private final By connectWalletButton = By.xpath(
            "//button[normalize-space()='Connect Wallet' or .//span[normalize-space()='Connect Wallet']]");

    // Sidebar - "Manage DIDs" link (visible after wallet connection)
    private final By manageDIDsLink = By.xpath(
            "//a[.//span[contains(text(),'Manage DIDs')] or contains(@href,'/dids')]");

    // Fallback: Manage DIDs "Explore" card link on the dashboard
    private final By manageDIDsCardLink = By.xpath(
            "//h4[contains(text(),'Manage DIDs')]/ancestor::div[contains(@class,'card') or contains(@class,'service')]"
                    +
                    "//a[contains(@href,'/dids')]");

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
    }

    /**
     * Navigates to the dashboard URL.
     */
    public void open(String baseUrl) {
        driver.get(baseUrl);
        System.out.println("[DashboardPage] Opened: " + baseUrl);
    }

    /**
     * Verifies Keplr has injected into the current page.
     * If false, Selenium is likely using a profile without Keplr installed.
     */
    public void assertKeplrInjected() {
        try {
            new WaitUtils(driver).waitForPresence(By.tagName("body"));
            Object injected = ((JavascriptExecutor) driver).executeScript(
                    "return typeof window.keplr !== 'undefined';");
            if (!(injected instanceof Boolean) || !((Boolean) injected)) {
                throw new IllegalStateException(
                        "Keplr was not injected into the page. Verify your selenium Chrome profile has Keplr installed.");
            }
            System.out.println("[DashboardPage] Keplr extension detected in page context.");
        } catch (Exception e) {
            throw new RuntimeException("Failed Keplr injection check: " + e.getMessage(), e);
        }
    }

    /**
     * Returns true if the wallet is already connected.
     * Detection strategy:
     * 1) If "Connect" or "Connect Wallet" is visible -> NOT connected.
     * 2) If "Manage DIDs" navigation is visible -> connected.
     * 3) Otherwise default to NOT connected (safe fallback).
     */
    public boolean isWalletConnected() {
        if (wait.isVisible(connectButtonHeader, 3)) {
            System.out.println("[DashboardPage] Wallet is NOT connected (header Connect visible).");
            return false;
        }

        if (wait.isVisible(connectWalletButton, 3)) {
            System.out.println("[DashboardPage] Wallet is NOT connected (Connect Wallet visible).");
            return false;
        }

        if (wait.isVisible(manageDIDsLink, 4) || wait.isVisible(manageDIDsCardLink, 4)) {
            System.out.println("[DashboardPage] Wallet appears CONNECTED (Manage DIDs UI visible).");
            return true;
        }

        System.out.println("[DashboardPage] Wallet state unclear. Defaulting to NOT connected.");
        return false;
    }

    /**
     * Clicks the main "Connect Wallet" button (hero section).
     * Falls back to the header "Connect" button if the main one isn't visible.
     */
    public boolean clickConnectWallet() {
        try {
            WebElement mainBtn = wait.waitForClickable(connectWalletButton, 10);
            mainBtn.click();
            System.out.println("[DashboardPage] Clicked 'Connect Wallet' (main body button).");
            return true;
        } catch (Exception ignored) {
            System.out.println("[DashboardPage] Main 'Connect Wallet' not found, trying header button...");
        }

        try {
            WebElement headerBtn = wait.waitForClickable(connectButtonHeader, 10);
            headerBtn.click();
            System.out.println("[DashboardPage] Clicked 'Connect' (header button).");
            return true;
        } catch (Exception e) {
            System.out.println("[DashboardPage] Connect buttons not clickable; wallet may already be connected.");
            return false;
        }
    }

    /**
     * Verifies the wallet is connected by checking the header button text.
     * The header button changes from "Connect" to the wallet address.
     */
    public void assertWalletConnected() {
        WaitUtils.sleep(500);

        boolean connected = isWalletConnected();
        if (!connected) {
            throw new AssertionError("[DashboardPage] ASSERTION FAILED: Wallet is still NOT connected after flow!");
        }
        System.out.println("[DashboardPage] ✅ Wallet connection VERIFIED.");
    }

    /**
     * Clicks on "Manage DIDs" from the sidebar (post-connection).
     * Falls back to the dashboard card link if the sidebar link isn't present.
     */
    public void navigateToManageDIDs() {
        try {
            WebElement sidebarLink = wait.waitForClickable(manageDIDsLink, 15);
            sidebarLink.click();
            System.out.println("[DashboardPage] Clicked 'Manage DIDs' sidebar link.");
        } catch (Exception e) {
            System.out.println("[DashboardPage] Sidebar link not found, trying card Explore link...");
            WebElement cardLink = wait.waitForClickable(manageDIDsCardLink, 10);
            cardLink.click();
            System.out.println("[DashboardPage] Clicked 'Manage DIDs' from card Explore link.");
        }
        wait.waitForUrl("/dids");
        System.out.println("[DashboardPage] Navigated to Manage DIDs page: " + driver.getCurrentUrl());
    }
}
