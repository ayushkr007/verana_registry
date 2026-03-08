package com.verana.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

/**
 * DriverManager
 *
 * Launches a fresh Chrome session with the Keplr extension loaded directly,
 * giving Selenium full control over the browser and extension popups.
 */
public class DriverManager {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final Properties config = loadConfig();

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties: " + e.getMessage(), e);
        }
        return props;
    }

    public static WebDriver getDriver() {
        if (driverThreadLocal.get() == null) {
            driverThreadLocal.set(createDriver());
        }
        return driverThreadLocal.get();
    }

    private static WebDriver createDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        // Anti-automation detection
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        // General Chrome flags
        options.addArguments("--no-sandbox");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--disable-renderer-backgrounding");

        // Page load strategy
        String pageLoadStrategyRaw = config.getProperty("page.load.strategy", "eager").trim().toLowerCase();
        PageLoadStrategy pageLoadStrategy;
        if ("none".equals(pageLoadStrategyRaw)) {
            pageLoadStrategy = PageLoadStrategy.NONE;
        } else if ("normal".equals(pageLoadStrategyRaw)) {
            pageLoadStrategy = PageLoadStrategy.NORMAL;
        } else {
            pageLoadStrategy = PageLoadStrategy.EAGER;
        }
        options.setPageLoadStrategy(pageLoadStrategy);

        // Chrome binary
        String chromeBinaryPath = config.getProperty("chrome.binary.path", "").trim();
        if (!chromeBinaryPath.isEmpty()) {
            options.setBinary(chromeBinaryPath);
        }

        // Persistent user-data-dir (keeps Keplr wallet data across runs)
        String userDataDir = config.getProperty("chrome.user.data.dir", "").trim();
        if (!userDataDir.isEmpty()) {
            Path userDataPath = Paths.get(userDataDir).toAbsolutePath();
            ensureDirectoryExists(userDataPath);
            options.addArguments("--user-data-dir=" + userDataPath);
            String profileDirectory = config.getProperty("chrome.profile.directory", "Default").trim();
            options.addArguments("--profile-directory=" + profileDirectory);
            System.out.println("[DriverManager] user-data-dir = " + userDataPath);
            System.out.println("[DriverManager] profile       = " + profileDirectory);
        }

        // Load Keplr extension
        String keplrExtPath = config.getProperty("keplr.extension.path", "").trim();
        if (!keplrExtPath.isEmpty()) {
            File extDir = new File(keplrExtPath);
            if (extDir.isDirectory() && new File(extDir, "manifest.json").exists()) {
                options.addArguments("--load-extension=" + extDir.getAbsolutePath());
                System.out.println("[DriverManager] Loading Keplr extension from: " + extDir.getAbsolutePath());
            } else if (extDir.isFile() && keplrExtPath.endsWith(".crx")) {
                options.addExtensions(extDir);
                System.out.println("[DriverManager] Loading Keplr .crx from: " + extDir.getAbsolutePath());
            } else {
                System.out.println("[DriverManager] WARNING: keplr.extension.path not valid: " + keplrExtPath);
            }
        } else {
            // Auto-detect from user-data-dir
            String extId = config.getProperty("keplr.extension.id", "dmkamcknogkgcdfhhbddcghachkejeap").trim();
            if (!userDataDir.isEmpty()) {
                String profileDir = config.getProperty("chrome.profile.directory", "Default").trim();
                Path extBase = Paths.get(userDataDir, profileDir, "Extensions", extId);
                if (extBase.toFile().isDirectory()) {
                    File[] versions = extBase.toFile().listFiles(File::isDirectory);
                    if (versions != null && versions.length > 0) {
                        // Use the latest version
                        File latestVersion = versions[versions.length - 1];
                        if (new File(latestVersion, "manifest.json").exists()) {
                            options.addArguments("--load-extension=" + latestVersion.getAbsolutePath());
                            System.out.println("[DriverManager] Auto-detected Keplr extension: " + latestVersion.getAbsolutePath());
                        }
                    }
                }
            }
        }

        boolean headless = Boolean.parseBoolean(config.getProperty("browser.headless", "false"));
        if (headless) {
            options.addArguments("--headless=new");
            options.addArguments("--window-size=1920,1080");
        }

        int implicitWait = Integer.parseInt(config.getProperty("implicit.wait.seconds", "10"));
        int pageLoadTimeout = Integer.parseInt(config.getProperty("page.load.timeout.seconds", "60"));

        System.out.println("[DriverManager] Launching Chrome (Selenium-owned session)...");
        System.out.println("[DriverManager] pageLoad = " + pageLoadStrategy);

        WebDriver driver;
        try {
            driver = new ChromeDriver(options);
        } catch (WebDriverException e) {
            throw new RuntimeException(
                    "Failed to launch Chrome. Make sure no other Chrome instances are using the same user-data-dir. " +
                    "Close all Chrome windows and retry.", e);
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeout));

        System.out.println("[DriverManager] Chrome session started. Current URL: " + driver.getCurrentUrl());
        return driver;
    }

    private static void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create Chrome user-data-dir: " + dir, e);
        }
    }

    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
            driverThreadLocal.remove();
        }
    }

    public static Properties getConfig() {
        return config;
    }
}
