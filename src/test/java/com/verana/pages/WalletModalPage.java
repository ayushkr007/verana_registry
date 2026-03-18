package com.verana.pages;

import com.verana.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Set;

/**
 * WalletModalPage
 *
 * Handles the "Select your wallet" modal and Keplr extension popup interactions.
 * Optimized for SPEED — Keplr popups only stay open for ~3-4 seconds.
 */
public class WalletModalPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    // ---- Locators ----

    private final By modalTitle = By.xpath(
            "//h2[contains(text(),'Select your wallet')] | //div[contains(text(),'Select your wallet')]");

    private final By keplrButton = By.xpath(
            "//button[@title='Keplr'] | //button[.//span[contains(text(),'Keplr')]] | " +
                    "//div[contains(@class,'wallet')][.//span[contains(text(),'Keplr')]]");

    // Approve/Confirm/Sign/Connect buttons (excludes reject/cancel)
    private final By keplrActionButton = By.xpath(
            "//button[not(@disabled) and " +
                    "not(contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'reject')) and " +
                    "not(contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'cancel')) and " +
                    "(" +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'approve') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'connect') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'confirm') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'sign') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'allow') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'grant') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'next') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'unlock')" +
                    ")]");

    private final By keplrPasswordInput = By.xpath(
            "//input[@type='password' or contains(translate(@placeholder, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'password')]");
    private final By keplrUnlockButton = By.xpath(
            "//button[not(@disabled) and (" +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'unlock') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'sign in') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'login') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'continue') or " +
                    "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'next')" +
                    ")]");

    private final boolean autoUnlockEnabled;
    private final String autoUnlockEnvVar;
    private String cachedKeplrPassword;
    private boolean loggedPasswordMissing;
    private boolean loggedPasswordLoaded;

    public WalletModalPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
        java.util.Properties config = com.verana.utils.DriverManager.getConfig();
        this.autoUnlockEnabled = Boolean.parseBoolean(config.getProperty("keplr.auto.unlock.enabled", "true"));
        this.autoUnlockEnvVar = config.getProperty("keplr.password.env.var", "KEPLR_PASSWORD").trim();
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    public void selectKeplr() {
        System.out.println("[Keplr] Waiting for wallet selection modal...");
        try {
            wait.waitForVisible(modalTitle, 15);
        } catch (Exception e) {
            System.out.println("[Keplr] Modal title not found, looking for Keplr button directly...");
        }
        WebElement keplr = wait.waitForClickable(keplrButton, 15);
        keplr.click();
        System.out.println("[Keplr] Clicked 'Keplr' wallet option.");
    }

    /**
     * Clicks Approve in the CURRENT window (caller already switched to Keplr popup).
     * Use this when you handle window switching yourself in the test.
     */
    public boolean clickApproveInCurrentWindow(int waitSeconds) {
        if (isWalletLocked()) {
            System.out.println("[Keplr] Wallet locked in current window. Unlocking...");
            attemptAutoUnlockIfLocked();
            WaitUtils.sleep(1000);
        }
        return fastClickApproveButton(waitSeconds);
    }

    /**
     * Approves the Keplr transaction after DID form submission.
     *
     * Strategy:
     * 1. Record current window handles
     * 2. Wait for a new Keplr popup window to appear
     * 3. Switch to it, find "Approve" button, click it
     * 4. If no popup window appears, open chrome-extension://popup.html in a new tab
     * 5. Switch back to the original window
     */
    public boolean approveKeplrTransaction(int waitSeconds) {
        String originalHandle;
        Set<String> handlesBefore;
        try {
            originalHandle = driver.getWindowHandle();
            handlesBefore = driver.getWindowHandles();
        } catch (Exception e) {
            System.out.println("[Keplr-TX] Cannot get window handles: " + e.getMessage());
            return false;
        }

        System.out.println("[Keplr-TX] Waiting for Keplr popup window (up to " + waitSeconds + "s)...");
        System.out.println("[Keplr-TX] Current windows: " + handlesBefore.size());

        // PHASE 1: Wait for Keplr to open a new popup window
        long deadline = System.currentTimeMillis() + (waitSeconds * 1000L);
        String keplrPopupHandle = null;

        while (System.currentTimeMillis() < deadline) {
            try {
                Set<String> currentHandles = driver.getWindowHandles();
                for (String handle : currentHandles) {
                    if (!handlesBefore.contains(handle)) {
                        keplrPopupHandle = handle;
                        break;
                    }
                }
                if (keplrPopupHandle != null) break;
            } catch (Exception ignored) {}
            WaitUtils.sleep(200);
        }

        // If a new window appeared, switch to it and click Approve
        if (keplrPopupHandle != null) {
            System.out.println("[Keplr-TX] New popup window detected. Switching...");
            driver.switchTo().window(keplrPopupHandle);
            WaitUtils.sleep(1000);

            // Unlock if needed
            if (isWalletLocked()) {
                System.out.println("[Keplr-TX] Wallet locked. Unlocking...");
                attemptAutoUnlockIfLocked();
                WaitUtils.sleep(1500);
            }

            // Find and click Approve
            boolean clicked = clickApproveButton(10);
            if (clicked) {
                System.out.println("[Keplr-TX] Approve clicked in popup window!");
                WaitUtils.sleep(1000);
                // Wait for popup to close, then switch back
                waitForWindowToClose(keplrPopupHandle, 5);
                switchBackTo(originalHandle);
                return true;
            } else {
                System.out.println("[Keplr-TX] Could not find Approve button in popup window.");
                dumpPageDiagnostics();
                switchBackTo(originalHandle);
            }
        } else {
            System.out.println("[Keplr-TX] No popup window appeared.");
        }

        // PHASE 2: Fallback — open Keplr popup.html in a new tab using driver.get()
        // window.open() doesn't properly load extension pages in MV3.
        // Instead, open a new tab via Selenium and navigate with driver.get().
        System.out.println("[Keplr-TX] Fallback: Opening Keplr popup.html in a new tab via driver.get()...");
        java.util.Properties config = com.verana.utils.DriverManager.getConfig();
        String extensionId = config.getProperty("keplr.extension.id", "dmkamcknogkgcdfhhbddcghachkejeap").trim();
        String keplrPopupUrl = "chrome-extension://" + extensionId + "/popup.html";

        String keplrTab = null;
        try {
            // Open a new tab using Selenium's built-in method
            driver.switchTo().newWindow(org.openqa.selenium.WindowType.TAB);
            keplrTab = driver.getWindowHandle();
            System.out.println("[Keplr-TX] New tab opened. Navigating to: " + keplrPopupUrl);

            // Navigate to the extension popup URL using driver.get()
            driver.get(keplrPopupUrl);
            WaitUtils.sleep(3000); // Give extension popup time to fully render

            System.out.println("[Keplr-TX] Current URL: " + driver.getCurrentUrl());
            System.out.println("[Keplr-TX] Page title: " + driver.getTitle());

            // Unlock if needed
            if (isWalletLocked()) {
                System.out.println("[Keplr-TX] Wallet locked in tab. Unlocking...");
                attemptAutoUnlockIfLocked();
                WaitUtils.sleep(1500);
            }

            // Log what's on the page for debugging
            try {
                String pageText = (String) ((JavascriptExecutor) driver).executeScript(
                        "return document.body ? document.body.innerText.substring(0, 500) : 'NO BODY';");
                System.out.println("[Keplr-TX] Page content: " + pageText);
            } catch (Exception ignored) {}

            // Find and click Approve
            boolean clicked = clickApproveButton(10);
            if (clicked) {
                System.out.println("[Keplr-TX] Approve clicked in Keplr tab!");
                WaitUtils.sleep(1000);
            } else {
                System.out.println("[Keplr-TX] Approve button not found. Dumping diagnostics...");
                dumpPageDiagnostics();
            }

            // Close the Keplr tab and switch back
            try {
                driver.close();
            } catch (Exception ignored) {}
            switchBackTo(originalHandle);
            return clicked;
        } catch (Exception e) {
            System.out.println("[Keplr-TX] Fallback failed: " + e.getMessage());
            e.printStackTrace();
            // Clean up tab if it was opened
            if (keplrTab != null) {
                try { driver.close(); } catch (Exception ignored) {}
            }
        }

        switchBackTo(originalHandle);
        return false;
    }

    /**
     * Finds and clicks the Approve button using WebDriverWait.
     * Tries multiple XPath strategies.
     */
    private boolean clickApproveButton(int waitSeconds) {
        long deadline = System.currentTimeMillis() + (waitSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            // Strategy 1: Button containing "Approve" text
            WebElement btn = findVisibleButton(By.xpath(
                    "//button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', " +
                    "'abcdefghijklmnopqrstuvwxyz'), 'approve')]"));

            // Strategy 2: Any positive action button (confirm, sign, etc.)
            if (btn == null) {
                btn = findVisibleButton(keplrActionButton);
            }

            if (btn != null) {
                String btnText = "";
                try { btnText = btn.getText().trim(); } catch (Exception ignored) {}
                System.out.println("[Keplr-TX] Found button: \"" + btnText + "\"");

                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView({block:'center'});", btn);
                } catch (Exception ignored) {}

                // Click it
                try {
                    btn.click();
                    System.out.println("[Keplr-TX] Clicked via element.click()");
                    return true;
                } catch (Exception e1) {
                    try {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                        System.out.println("[Keplr-TX] Clicked via JS click");
                        return true;
                    } catch (Exception e2) {
                        System.out.println("[Keplr-TX] Click failed: " + e2.getMessage());
                    }
                }
            }

            WaitUtils.sleep(500);
        }
        return false;
    }

    /**
     * Finds a visible, enabled button matching the given locator.
     */
    private WebElement findVisibleButton(By locator) {
        try {
            List<WebElement> buttons = driver.findElements(locator);
            for (WebElement btn : buttons) {
                if (btn.isDisplayed() && btn.isEnabled()) return btn;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Waits for Keplr popup, switches to it, clicks Approve, then switches back.
     * Used during wallet connection in setUp.
     */
    public int approveInKeplrPopup(int maxActions, int waitSeconds) {
        String originalHandle;
        Set<String> handlesBefore;
        try {
            originalHandle = driver.getWindowHandle();
            handlesBefore = driver.getWindowHandles();
        } catch (Exception e) {
            System.out.println("[Keplr] Cannot get window handles: " + e.getMessage());
            return 0;
        }

        System.out.println("[Keplr] Waiting for Keplr popup (up to " + waitSeconds + "s)...");
        int totalClicked = 0;
        long overallDeadline = System.currentTimeMillis() + (waitSeconds * 1000L);

        // ── PHASE 1: Wait for Keplr auto-popup window, click FAST ──
        while (totalClicked < maxActions && System.currentTimeMillis() < overallDeadline) {

            String keplrPopupHandle = waitForNewWindow(handlesBefore, overallDeadline);

            if (keplrPopupHandle == null) {
                System.out.println("[Keplr] Phase 1: No popup window detected.");
                break;
            }

            driver.switchTo().window(keplrPopupHandle);
            System.out.println("[Keplr] Phase 1: Switched to popup.");

            // Minimal wait — just enough for DOM to render
            WaitUtils.sleep(500);

            if (isWalletLocked()) {
                System.out.println("[Keplr] Wallet locked. Unlocking...");
                attemptAutoUnlockIfLocked();
                WaitUtils.sleep(1500);
            }

            // FAST click — try everything rapidly
            boolean clicked = fastClickApproveButton(8);

            if (clicked) {
                totalClicked++;
                System.out.println("[Keplr] Phase 1: Approve clicked! (total: " + totalClicked + ")");
                WaitUtils.sleep(500);
            } else {
                dumpPageDiagnostics();
                System.out.println("[Keplr] Phase 1: Could not click Approve.");
            }

            waitForWindowToClose(keplrPopupHandle, 3);
            handlesBefore = driver.getWindowHandles();
            switchBackTo(originalHandle);
        }

        // ── PHASE 2: Open popup.html in a new tab as fallback ──
        if (totalClicked == 0 && System.currentTimeMillis() < overallDeadline) {
            System.out.println("[Keplr] Phase 2: Opening popup.html in a new tab...");

            java.util.Properties config = com.verana.utils.DriverManager.getConfig();
            String extensionId = config.getProperty("keplr.extension.id", "dmkamcknogkgcdfhhbddcghachkejeap").trim();
            String keplrPopupUrl = "chrome-extension://" + extensionId + "/popup.html";

            String keplrTab = null;
            try {
                Set<String> existingHandles = driver.getWindowHandles();
                ((JavascriptExecutor) driver).executeScript("window.open('about:blank','_blank');");
                keplrTab = wait.waitForNewWindow(existingHandles, 3);
                driver.switchTo().window(keplrTab);
            } catch (Exception e) {
                System.out.println("[Keplr] Phase 2: Failed to open tab: " + e.getMessage());
                switchBackTo(originalHandle);
                return totalClicked;
            }

            try {
                driver.get(keplrPopupUrl);
                System.out.println("[Keplr] Phase 2: Navigated to " + keplrPopupUrl);
                WaitUtils.sleep(1500);

                if (isWalletLocked()) {
                    attemptAutoUnlockIfLocked();
                    WaitUtils.sleep(1500);
                }

                int remaining = (int) ((overallDeadline - System.currentTimeMillis()) / 1000);
                boolean clicked = fastClickApproveButton(Math.max(remaining, 5));

                if (clicked) {
                    totalClicked++;
                    System.out.println("[Keplr] Phase 2: Approve clicked!");
                    WaitUtils.sleep(500);
                } else {
                    dumpPageDiagnostics();
                    System.out.println("[Keplr] Phase 2: Could not click Approve.");
                }
            } catch (Exception e) {
                System.out.println("[Keplr] Phase 2 error: " + e.getMessage());
            }

            try {
                if (keplrTab != null && driver.getWindowHandles().contains(keplrTab)) {
                    driver.switchTo().window(keplrTab);
                    driver.close();
                }
            } catch (Exception ignored) {}
            switchBackTo(originalHandle);
        }

        switchBackTo(originalHandle);
        System.out.println("[Keplr] Done. Total actions clicked: " + totalClicked);
        return totalClicked;
    }

    public boolean tryAutoUnlockInOpenContexts(int seconds) {
        long deadline = System.currentTimeMillis() + (Math.max(1, seconds) * 1000L);
        String originalHandle;
        try {
            originalHandle = driver.getWindowHandle();
        } catch (Exception e) {
            return false;
        }

        System.out.println("[Keplr] tryAutoUnlockInOpenContexts: scanning windows for " + seconds + "s...");
        int attempt = 0;

        while (System.currentTimeMillis() < deadline) {
            Set<String> handles;
            try {
                handles = driver.getWindowHandles();
            } catch (Exception e) {
                return false;
            }

            for (String handle : handles) {
                try {
                    driver.switchTo().window(handle);
                    String url = "";
                    try { url = driver.getCurrentUrl(); } catch (Exception ignored) {}

                    if (isWalletLocked()) {
                        attempt++;
                        System.out.println("[Keplr] Wallet locked detected in window (URL: " + url + "), attempt #" + attempt);
                        // Wait for page to fully render before trying password
                        WaitUtils.sleep(500);

                        if (attemptAutoUnlockIfLocked()) {
                            // Give Keplr time to process the unlock
                            WaitUtils.sleep(1000);
                            if (!isWalletLocked()) {
                                System.out.println("[Keplr] Wallet UNLOCKED successfully!");
                                switchBackTo(originalHandle);
                                return true;
                            } else {
                                System.out.println("[Keplr] Unlock attempt returned true but wallet still appears locked. Retrying...");
                            }
                        } else {
                            System.out.println("[Keplr] attemptAutoUnlockIfLocked returned false.");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[Keplr] Error in window scan: " + e.getMessage());
                }
            }
            WaitUtils.sleep(300);
        }

        System.out.println("[Keplr] tryAutoUnlockInOpenContexts: timed out after " + seconds + "s, " + attempt + " attempts.");
        switchBackTo(originalHandle);
        return false;
    }

    public boolean tryApproveTransactionRequestQuick() {
        int quickWait = Integer.parseInt(
                com.verana.utils.DriverManager.getConfig().getProperty("keplr.transaction.quick.popup.wait.seconds", "2"));
        return tryApproveInAnyWindow(quickWait);
    }

    public void approveTransactionRequest() {
        int waitSeconds = Integer.parseInt(
                com.verana.utils.DriverManager.getConfig().getProperty("keplr.transaction.popup.wait.seconds", "20"));
        if (!tryApproveInAnyWindow(waitSeconds)) {
            throw new RuntimeException("Keplr approval popup did not appear within " + waitSeconds + " seconds.");
        }
    }

    // =========================================================================
    // CORE: Fast popup detection and Approve button click
    // =========================================================================

    /**
     * Polls for a new window handle every 100ms (fast).
     */
    private String waitForNewWindow(Set<String> handlesBefore, long deadlineMillis) {
        while (System.currentTimeMillis() < deadlineMillis) {
            try {
                Set<String> currentHandles = driver.getWindowHandles();
                for (String handle : currentHandles) {
                    if (!handlesBefore.contains(handle)) {
                        return handle;
                    }
                }
            } catch (Exception ignored) {}
            WaitUtils.sleep(100); // Fast polling — every 100ms
        }
        return null;
    }

    /**
     * FAST approve: find button and blast ALL click methods immediately.
     * No slow verification between attempts. The popup is only alive for 3-4s.
     * We fire every click method we have, then check if it worked.
     */
    private boolean fastClickApproveButton(int waitSeconds) {
        long deadline = System.currentTimeMillis() + (waitSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            WebElement btn = findApproveButtonFast();

            if (btn != null) {
                String btnText = "";
                try { btnText = btn.getText().trim(); } catch (Exception ignored) {}
                System.out.println("[Keplr] Found button: \"" + btnText + "\". Blasting all click methods...");

                // Scroll into view
                try {
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView({block:'center',behavior:'instant'});", btn);
                } catch (Exception ignored) {}

                // BLAST: Fire ALL click strategies rapidly — no waiting between them
                // Strategy 1: Selenium Actions (real mouse events)
                try {
                    new Actions(driver).moveToElement(btn).click().perform();
                    System.out.println("[Keplr] Fired: Actions click");
                } catch (Exception e) {
                    System.out.println("[Keplr] Actions failed: " + e.getMessage());
                }

                // Strategy 2: element.click()
                try {
                    btn.click();
                    System.out.println("[Keplr] Fired: element.click()");
                } catch (Exception e) {
                    System.out.println("[Keplr] element.click() failed: " + e.getMessage());
                }

                // Strategy 3: JavaScript .click()
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
                    System.out.println("[Keplr] Fired: JS click");
                } catch (Exception ignored) {}

                // Strategy 4: Keyboard Enter
                try {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", btn);
                    btn.sendKeys(Keys.ENTER);
                    System.out.println("[Keplr] Fired: Enter key");
                } catch (Exception ignored) {}

                // Strategy 5: Robot class — OS-level native mouse click (skipped in headless / CI)
                if (!Boolean.parseBoolean(com.verana.utils.DriverManager.getConfig().getProperty("browser.headless", "false"))
                        && !java.awt.GraphicsEnvironment.isHeadless()) {
                    try {
                        org.openqa.selenium.Point loc = btn.getLocation();
                        org.openqa.selenium.Dimension size = btn.getSize();
                        org.openqa.selenium.Point windowPos = driver.manage().window().getPosition();
                        int screenX = windowPos.getX() + loc.getX() + size.getWidth() / 2;
                        int screenY = windowPos.getY() + loc.getY() + size.getHeight() / 2 + 85;
                        Robot robot = new Robot();
                        robot.mouseMove(screenX, screenY);
                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                        System.out.println("[Keplr] Fired: Robot click at (" + screenX + "," + screenY + ")");
                    } catch (Exception e) {
                        System.out.println("[Keplr] Robot click failed: " + e.getMessage());
                    }
                } else {
                    System.out.println("[Keplr] Skipped Robot click (headless/CI mode).");
                }

                // Strategy 6: CDP Input.dispatchMouseEvent
                try {
                    org.openqa.selenium.Point loc = btn.getLocation();
                    org.openqa.selenium.Dimension size = btn.getSize();
                    int clickX = loc.getX() + size.getWidth() / 2;
                    int clickY = loc.getY() + size.getHeight() / 2;
                    org.openqa.selenium.chrome.ChromeDriver cdpDriver = (org.openqa.selenium.chrome.ChromeDriver) driver;
                    java.util.Map<String, Object> params = new java.util.HashMap<>();
                    params.put("type", "mousePressed");
                    params.put("x", clickX);
                    params.put("y", clickY);
                    params.put("button", "left");
                    params.put("clickCount", 1);
                    cdpDriver.executeCdpCommand("Input.dispatchMouseEvent", params);
                    params.put("type", "mouseReleased");
                    cdpDriver.executeCdpCommand("Input.dispatchMouseEvent", params);
                    System.out.println("[Keplr] Fired: CDP click at (" + clickX + "," + clickY + ")");
                } catch (Exception e) {
                    System.out.println("[Keplr] CDP click failed: " + e.getMessage());
                }

                // Now check if ANY of those worked
                WaitUtils.sleep(500);

                // Check: button gone = success
                try {
                    if (!btn.isDisplayed()) {
                        System.out.println("[Keplr] VERIFIED: button disappeared — click worked!");
                        return true;
                    }
                } catch (org.openqa.selenium.StaleElementReferenceException e) {
                    System.out.println("[Keplr] VERIFIED: button became stale — click worked!");
                    return true;
                } catch (Exception e) {
                    System.out.println("[Keplr] VERIFIED: button inaccessible — click worked!");
                    return true;
                }

                // Check: can we still find any approve button?
                if (findApproveButtonFast() == null) {
                    System.out.println("[Keplr] VERIFIED: no approve button found — click worked!");
                    return true;
                }

                System.out.println("[Keplr] Button still present after all strategies. Retrying...");
            }

            WaitUtils.sleep(200);
        }
        return false;
    }

    /**
     * FAST button finder — no WebDriverWait timeouts, instant checks only.
     */
    private WebElement findApproveButtonFast() {
        // Strategy 1: Direct XPath for "Approve" button (instant, no wait)
        try {
            List<WebElement> buttons = driver.findElements(By.xpath(
                    "//button[contains(translate(normalize-space(.), " +
                    "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'approve')]"));
            for (WebElement btn : buttons) {
                if (btn.isDisplayed() && btn.isEnabled()) return btn;
            }
        } catch (Exception ignored) {}

        // Strategy 2: Any positive action button
        try {
            List<WebElement> buttons = driver.findElements(keplrActionButton);
            for (WebElement btn : buttons) {
                if (btn.isDisplayed() && btn.isEnabled()) return btn;
            }
        } catch (Exception ignored) {}

        // Strategy 3: JS search including shadow DOM
        try {
            WebElement btn = (WebElement) ((JavascriptExecutor) driver).executeScript(
                    "var actions = ['approve','confirm','sign','connect','allow','next'];" +
                    "var exclude = ['reject','cancel','deny','close','back'];" +
                    "function search(root) {" +
                    "  var els = root.querySelectorAll('button, [role=\"button\"]');" +
                    "  for (var i = 0; i < els.length; i++) {" +
                    "    var txt = (els[i].innerText || els[i].textContent || '').trim().toLowerCase();" +
                    "    if (!txt) continue;" +
                    "    var bad = false;" +
                    "    for (var j = 0; j < exclude.length; j++) { if (txt.indexOf(exclude[j]) >= 0) { bad = true; break; } }" +
                    "    if (bad) continue;" +
                    "    for (var j = 0; j < actions.length; j++) {" +
                    "      if (txt.indexOf(actions[j]) >= 0) {" +
                    "        var rect = els[i].getBoundingClientRect();" +
                    "        if (rect.width > 0 && rect.height > 0 && !els[i].disabled) return els[i];" +
                    "      }" +
                    "    }" +
                    "  }" +
                    "  var all = root.querySelectorAll('*');" +
                    "  for (var i = 0; i < all.length; i++) {" +
                    "    if (all[i].shadowRoot) {" +
                    "      var found = search(all[i].shadowRoot);" +
                    "      if (found) return found;" +
                    "    }" +
                    "  }" +
                    "  return null;" +
                    "}" +
                    "return search(document);");
            if (btn != null) return btn;
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Tries to approve in any currently open window.
     */
    private boolean tryApproveInAnyWindow(int waitSeconds) {
        String originalHandle;
        try {
            originalHandle = driver.getWindowHandle();
        } catch (Exception e) {
            return false;
        }

        long deadline = System.currentTimeMillis() + (waitSeconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            Set<String> handles = driver.getWindowHandles();
            for (String handle : handles) {
                try {
                    driver.switchTo().window(handle);
                    String url = driver.getCurrentUrl();
                    if (url != null && url.contains("chrome-extension://")) {
                        if (isWalletLocked()) {
                            attemptAutoUnlockIfLocked();
                            WaitUtils.sleep(500);
                        }
                        if (fastClickApproveButton(3)) {
                            switchBackTo(originalHandle);
                            return true;
                        }
                    }
                } catch (Exception ignored) {}
            }
            WaitUtils.sleep(200);
        }

        switchBackTo(originalHandle);
        return false;
    }

    // =========================================================================
    // UNLOCK helpers
    // =========================================================================

    private boolean isWalletLocked() {
        try {
            List<WebElement> passwordInputs = driver.findElements(keplrPasswordInput);
            for (WebElement input : passwordInputs) {
                if (input.isDisplayed()) return true;
            }
            return hasPasswordFieldInShadowDom();
        } catch (NoSuchWindowException e) {
            return false;
        }
    }

    private boolean hasPasswordFieldInShadowDom() {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                    "var roots=[document];" +
                    "var seen=[];" +
                    "while(roots.length){" +
                    "  var root=roots.shift();" +
                    "  if(!root || seen.indexOf(root)>=0) continue;" +
                    "  seen.push(root);" +
                    "  var input=root.querySelector('input[type=\"password\"]');" +
                    "  if(input) return true;" +
                    "  var all=root.querySelectorAll('*');" +
                    "  for(var i=0;i<all.length;i++){ if(all[i].shadowRoot) roots.push(all[i].shadowRoot); }" +
                    "}" +
                    "return false;");
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean attemptAutoUnlockIfLocked() {
        if (!autoUnlockEnabled) return false;
        String password = resolveKeplrPassword();
        if (password == null || password.isEmpty()) return false;

        if (enterPasswordInVisibleInput(password)) {
            System.out.println("[Keplr] Auto-unlock: entered password in visible input.");
            return true;
        }
        if (enterPasswordInShadowDom(password)) {
            System.out.println("[Keplr] Auto-unlock: entered password in shadow DOM.");
            return true;
        }
        return false;
    }

    private boolean enterPasswordInVisibleInput(String password) {
        try {
            List<WebElement> inputs = driver.findElements(keplrPasswordInput);
            for (WebElement input : inputs) {
                if (!input.isDisplayed() || !input.isEnabled()) continue;

                System.out.println("[Keplr] Found visible password input. Attempting to enter password...");

                // Strategy 1: Click + clear via JS native setter + sendKeys (best for React)
                boolean entered = false;
                try {
                    input.click();
                    WaitUtils.sleep(100);
                    // Use React-compatible native setter to clear
                    ((JavascriptExecutor) driver).executeScript(
                            "var nativeSetter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                            "nativeSetter.call(arguments[0],'');" +
                            "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                            "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                            input);
                    input.sendKeys(password);
                    WaitUtils.sleep(100);
                    // Fire events so React picks up the value
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                            "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                            input);
                    String val = input.getAttribute("value");
                    if (val != null && !val.isEmpty()) {
                        entered = true;
                        System.out.println("[Keplr] Strategy 1 (sendKeys): password entered (length=" + val.length() + ")");
                    }
                } catch (Exception e) {
                    System.out.println("[Keplr] Strategy 1 (sendKeys) failed: " + e.getMessage());
                }

                // Strategy 2: Full JS — set value via native setter + dispatch all React events
                if (!entered) {
                    try {
                        ((JavascriptExecutor) driver).executeScript(
                                "var el = arguments[0], val = arguments[1];" +
                                "el.focus();" +
                                "var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;" +
                                "nativeSetter.call(el, '');" +
                                "nativeSetter.call(el, val);" +
                                "el.dispatchEvent(new Event('input', {bubbles: true}));" +
                                "el.dispatchEvent(new Event('change', {bubbles: true}));" +
                                // Also dispatch React synthetic-like events
                                "el.dispatchEvent(new InputEvent('input', {bubbles: true, data: val, inputType: 'insertText'}));" +
                                "el.dispatchEvent(new Event('blur', {bubbles: true}));",
                                input, password);
                        WaitUtils.sleep(100);
                        String val = input.getAttribute("value");
                        if (val != null && !val.isEmpty()) {
                            entered = true;
                            System.out.println("[Keplr] Strategy 2 (JS native setter): password entered (length=" + val.length() + ")");
                        }
                    } catch (Exception e) {
                        System.out.println("[Keplr] Strategy 2 (JS native setter) failed: " + e.getMessage());
                    }
                }

                // Strategy 3: Actions class — simulate real keyboard input character by character
                if (!entered) {
                    try {
                        Keys selectAllKey = System.getProperty("os.name", "").toLowerCase().contains("mac")
                            ? Keys.COMMAND : Keys.CONTROL;
                    new Actions(driver)
                                .click(input)
                                .keyDown(selectAllKey).sendKeys("a").keyUp(selectAllKey)
                                .sendKeys(Keys.BACK_SPACE)
                                .sendKeys(password)
                                .perform();
                        WaitUtils.sleep(100);
                        String val = input.getAttribute("value");
                        if (val != null && !val.isEmpty()) {
                            entered = true;
                            System.out.println("[Keplr] Strategy 3 (Actions): password entered (length=" + val.length() + ")");
                        }
                    } catch (Exception e) {
                        System.out.println("[Keplr] Strategy 3 (Actions) failed: " + e.getMessage());
                    }
                }

                // Strategy 4: CDP Input.dispatchKeyEvent — type each character via Chrome DevTools Protocol
                if (!entered) {
                    try {
                        input.click();
                        WaitUtils.sleep(50);
                        org.openqa.selenium.chrome.ChromeDriver cdpDriver = (org.openqa.selenium.chrome.ChromeDriver) driver;
                        for (char c : password.toCharArray()) {
                            java.util.Map<String, Object> keyDown = new java.util.HashMap<>();
                            keyDown.put("type", "keyDown");
                            keyDown.put("text", String.valueOf(c));
                            keyDown.put("key", String.valueOf(c));
                            cdpDriver.executeCdpCommand("Input.dispatchKeyEvent", keyDown);

                            java.util.Map<String, Object> keyUp = new java.util.HashMap<>();
                            keyUp.put("type", "keyUp");
                            keyUp.put("key", String.valueOf(c));
                            cdpDriver.executeCdpCommand("Input.dispatchKeyEvent", keyUp);
                        }
                        WaitUtils.sleep(100);
                        String val = input.getAttribute("value");
                        if (val != null && !val.isEmpty()) {
                            entered = true;
                            System.out.println("[Keplr] Strategy 4 (CDP keyEvents): password entered (length=" + val.length() + ")");
                        }
                    } catch (Exception e) {
                        System.out.println("[Keplr] Strategy 4 (CDP keyEvents) failed: " + e.getMessage());
                    }
                }

                if (!entered) {
                    System.out.println("[Keplr] WARNING: All password entry strategies failed for visible input.");
                    return false;
                }

                // Now click the Unlock button
                WaitUtils.sleep(200);
                try {
                    List<WebElement> unlockButtons = driver.findElements(keplrUnlockButton);
                    for (WebElement button : unlockButtons) {
                        if (button.isDisplayed() && button.isEnabled()) {
                            System.out.println("[Keplr] Clicking unlock button: " + button.getText().trim());
                            safeClick(button);
                            return true;
                        }
                    }
                } catch (Exception ignored) {}

                // Fallback: press Enter on the input
                System.out.println("[Keplr] No unlock button found, pressing Enter...");
                input.sendKeys(Keys.ENTER);
                return true;
            }
        } catch (Exception e) {
            System.out.println("[Keplr] enterPasswordInVisibleInput error: " + e.getMessage());
        }
        return false;
    }

    private boolean enterPasswordInShadowDom(String password) {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                    "var pass = arguments[0];" +
                    "var roots=[document];" +
                    "var seen=[];" +
                    "var isVisible=function(el){" +
                    "  if(!el) return false;" +
                    "  var s=getComputedStyle(el);" +
                    "  if(s.visibility==='hidden' || s.display==='none') return false;" +
                    "  var r=el.getBoundingClientRect();" +
                    "  return r.width>0 && r.height>0;" +
                    "};" +
                    "var setReactValue=function(input, val){" +
                    "  try{" +
                    "    var nativeSetter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;" +
                    "    nativeSetter.call(input, val);" +
                    "  } catch(e){ input.value=val; }" +
                    "  input.dispatchEvent(new Event('input',{bubbles:true}));" +
                    "  input.dispatchEvent(new Event('change',{bubbles:true}));" +
                    "};" +
                    "while(roots.length){" +
                    "  var root=roots.shift();" +
                    "  if(!root || seen.indexOf(root)>=0) continue;" +
                    "  seen.push(root);" +
                    "  var input=root.querySelector('input[type=\"password\"]');" +
                    "  if(input && isVisible(input)){" +
                    "    input.focus();" +
                    "    setReactValue(input,'');" +
                    "    setReactValue(input,pass);" +
                    "    var btns=root.querySelectorAll('button,[role=\"button\"],input[type=\"submit\"]');" +
                    "    for(var i=0;i<btns.length;i++){" +
                    "      var t=(btns[i].innerText||btns[i].textContent||btns[i].value||'').toLowerCase();" +
                    "      if((t.indexOf('unlock')>=0||t.indexOf('sign in')>=0||t.indexOf('login')>=0||t.indexOf('continue')>=0||t.indexOf('next')>=0) && !btns[i].disabled && isVisible(btns[i])){" +
                    "        btns[i].click(); return true;" +
                    "      }" +
                    "    }" +
                    "    var evt=new KeyboardEvent('keydown',{key:'Enter',code:'Enter',which:13,keyCode:13,bubbles:true});" +
                    "    input.dispatchEvent(evt);" +
                    "    return true;" +
                    "  }" +
                    "  var all=root.querySelectorAll('*');" +
                    "  for(var i=0;i<all.length;i++){ if(all[i].shadowRoot) roots.push(all[i].shadowRoot); }" +
                    "}" +
                    "return false;",
                    password);
            return Boolean.TRUE.equals(result);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String resolveKeplrPassword() {
        if (cachedKeplrPassword != null) return cachedKeplrPassword;

        String envName = (autoUnlockEnvVar == null || autoUnlockEnvVar.isBlank()) ? "KEPLR_PASSWORD" : autoUnlockEnvVar;
        String value = System.getenv(envName);

        if (value == null || value.trim().isEmpty()) {
            if (!loggedPasswordMissing) {
                loggedPasswordMissing = true;
                System.out.println("[Keplr] ============================================================");
                System.out.println("[Keplr] ERROR: Keplr password not set!");
                System.out.println("[Keplr] Set the environment variable before running tests:");
                System.out.println("[Keplr]   export " + envName + "=YourKeplrPassword");
                System.out.println("[Keplr] ============================================================");
            }
            return null;
        }

        cachedKeplrPassword = value.trim();
        if (!loggedPasswordLoaded) {
            loggedPasswordLoaded = true;
            System.out.println("[Keplr] Password loaded from env variable '" + envName + "'.");
        }
        return cachedKeplrPassword;
    }

    // =========================================================================
    // UTILITY helpers
    // =========================================================================

    private void safeClick(WebElement element) {
        try {
            element.click();
        } catch (Exception clickError) {
            try {
                new Actions(driver).moveToElement(element).click().perform();
            } catch (Exception actionsError) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            }
        }
    }

    private void switchBackTo(String handle) {
        try {
            if (driver.getWindowHandles().contains(handle)) {
                driver.switchTo().window(handle);
            }
        } catch (Exception ignored) {}
    }

    private void waitForWindowToClose(String handle, int seconds) {
        long deadline = System.currentTimeMillis() + (seconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!driver.getWindowHandles().contains(handle)) return;
            } catch (Exception e) {
                return;
            }
            WaitUtils.sleep(200);
        }
    }

    private void dumpPageDiagnostics() {
        try {
            String url = driver.getCurrentUrl();
            String title = driver.getTitle();
            System.out.println("[Keplr] DIAGNOSTIC - URL: " + url);
            System.out.println("[Keplr] DIAGNOSTIC - Title: " + title);

            Object buttonsInfo = ((JavascriptExecutor) driver).executeScript(
                    "var result = [];" +
                    "var allEls = document.querySelectorAll('button, [role=\"button\"], input[type=\"submit\"], a');" +
                    "for (var i = 0; i < allEls.length; i++) {" +
                    "  var el = allEls[i];" +
                    "  var txt = (el.innerText || el.textContent || el.value || '').trim();" +
                    "  var rect = el.getBoundingClientRect();" +
                    "  var visible = rect.width > 0 && rect.height > 0;" +
                    "  if (txt) result.push(el.tagName + ' text=\"' + txt.substring(0,50) + '\" visible=' + visible + ' disabled=' + el.disabled);" +
                    "}" +
                    "return result.join('\\n');");
            System.out.println("[Keplr] DIAGNOSTIC - Buttons:\n" + buttonsInfo);

            Object bodyText = ((JavascriptExecutor) driver).executeScript(
                    "return (document.body ? document.body.innerText : '').substring(0, 500);");
            System.out.println("[Keplr] DIAGNOSTIC - Page text:\n" + bodyText);

            try {
                java.io.File screenshotFile = ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
                java.io.File dest = new java.io.File(System.getProperty("user.dir"), "keplr_debug_screenshot.png");
                java.nio.file.Files.copy(screenshotFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[Keplr] DIAGNOSTIC - Screenshot: " + dest.getAbsolutePath());
            } catch (Exception screenshotErr) {
                System.out.println("[Keplr] DIAGNOSTIC - Screenshot failed: " + screenshotErr.getMessage());
            }
        } catch (Exception e) {
            System.out.println("[Keplr] DIAGNOSTIC failed: " + e.getMessage());
        }
    }
}
