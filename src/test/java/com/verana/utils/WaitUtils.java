package com.verana.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Set;

/**
 * WaitUtils
 *
 * Centralized explicit wait utilities to avoid flaky tests.
 * Uses WebDriverWait with configurable timeouts.
 */
public class WaitUtils {

    private final WebDriver driver;
    private final int defaultWaitSeconds;

    public WaitUtils(WebDriver driver) {
        this.driver = driver;
        this.defaultWaitSeconds = Integer.parseInt(
                DriverManager.getConfig().getProperty("explicit.wait.seconds", "30"));
    }

    /**
     * Waits until the element located by the given By is visible on the page.
     */
    public WebElement waitForVisible(By locator) {
        return waitForVisible(locator, defaultWaitSeconds);
    }

    public WebElement waitForVisible(By locator, int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits until the element is clickable (visible + enabled).
     */
    public WebElement waitForClickable(By locator) {
        return waitForClickable(locator, defaultWaitSeconds);
    }

    public WebElement waitForClickable(By locator, int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits until the element is present in the DOM (not necessarily visible).
     */
    public WebElement waitForPresence(By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(defaultWaitSeconds))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public boolean isVisible(By locator, int seconds) {
        try {
            waitForVisible(locator, seconds);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits until the current URL contains the given partial string.
     */
    public void waitForUrl(String partialUrl) {
        new WebDriverWait(driver, Duration.ofSeconds(defaultWaitSeconds))
                .until(ExpectedConditions.urlContains(partialUrl));
    }

    public String waitForNewWindow(Set<String> existingHandles, int seconds) {
        new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(d -> d.getWindowHandles().size() > existingHandles.size());

        for (String handle : driver.getWindowHandles()) {
            if (!existingHandles.contains(handle)) {
                return handle;
            }
        }
        throw new RuntimeException("No new window found to switch to.");
    }

    public boolean waitUntilWindowClosed(String windowHandle, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(d -> !d.getWindowHandles().contains(windowHandle));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Simple sleep - use sparingly, prefer explicit waits.
     */
    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
