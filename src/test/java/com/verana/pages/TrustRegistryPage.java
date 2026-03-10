package com.verana.pages;

import com.verana.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.List;

/**
 * TrustRegistryPage
 *
 * Represents the Trust Registry page (/tr).
 * Handles clicking "Create Ecosystem", filling the form fields
 * (DID, Aka, Primary Governance Framework Language, Governance Framework Primary Document URL),
 * and submitting.
 */
public class TrustRegistryPage {

    private final WebDriver driver;
    private final WaitUtils wait;

    // ---- Locators ----

    // "Create Ecosystem" button on the TR page
    private final By createEcosystemButton = By.xpath(
            "//button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'create ecosystem')] | " +
            "//button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'create an ecosystem')] | " +
            "//a[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'create ecosystem')]");

    // DID input - label "DID*" -> following input with placeholder "did:method:identifier"
    private final By didInput = By.xpath(
            "//input[@placeholder='did:method:identifier'] | " +
            "//label[starts-with(normalize-space(.), 'DID')]/following-sibling::*//input | " +
            "//label[starts-with(normalize-space(.), 'DID')]/following::input[1]");

    // Aka input - label "Aka*" -> following input (NOT the search bar)
    private final By akaInput = By.xpath(
            "//label[starts-with(normalize-space(.), 'Aka')]/following-sibling::*//input | " +
            "//label[starts-with(normalize-space(.), 'Aka')]/following::input[1]");

    // Primary Governance Framework Language - it's a native <select>
    private final By governanceLanguageSelect = By.xpath(
            "//label[contains(normalize-space(.), 'Primary Governance Framework Language')]/following-sibling::*//select | " +
            "//label[contains(normalize-space(.), 'Primary Governance Framework Language')]/following::select[1]");

    // Governance Framework Primary Document URL input
    private final By governanceDocUrlInput = By.xpath(
            "//label[contains(normalize-space(.), 'Governance Framework Primary Document URL')]/following-sibling::*//input | " +
            "//label[contains(normalize-space(.), 'Governance Framework Primary Document URL')]/following::input[1]");

    // Confirm button after form is filled
    private final By confirmButton = By.xpath(
            "//button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'confirm')] | " +
            "//button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'submit')] | " +
            "//button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'create')] | " +
            "//button[@type='submit']");

    // Transaction success indicators
    private final By transactionSuccessMessage = By.xpath(
            "//*[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'transaction successful') or " +
            "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'successful transaction') or " +
            "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'transaction in progress') or " +
            "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'creating ecosystem') or " +
            "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'ecosystem created') or " +
            "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'tx hash') or " +
            "contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'success')]");

    public TrustRegistryPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WaitUtils(driver);
    }

    /**
     * Checks if the "Create Ecosystem" button is visible.
     */
    public boolean isCreateEcosystemButtonVisible(int seconds) {
        return wait.isVisible(createEcosystemButton, seconds);
    }

    /**
     * Clicks the "Create Ecosystem" button.
     */
    public void clickCreateEcosystem() {
        System.out.println("[TrustRegistryPage] Looking for 'Create Ecosystem' button...");
        WebElement btn = wait.waitForClickable(createEcosystemButton, 10);
        scrollIntoView(btn);
        click(btn);
        System.out.println("[TrustRegistryPage] Clicked 'Create Ecosystem' button.");
        WaitUtils.sleep(1500);
    }

    /**
     * Dumps all visible labels, inputs, selects, and textareas on the page for debugging.
     */
    public void dumpFormFields() {
        System.out.println("[TrustRegistryPage] === FORM FIELD DIAGNOSTIC ===");
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                    "var out = [];" +
                    "var labels = document.querySelectorAll('label');" +
                    "for (var i = 0; i < labels.length; i++) {" +
                    "  var l = labels[i];" +
                    "  var rect = l.getBoundingClientRect();" +
                    "  if (rect.width > 0 && rect.height > 0) {" +
                    "    out.push('LABEL: \"' + l.innerText.trim().substring(0,80) + '\" for=' + (l.htmlFor||'none'));" +
                    "  }" +
                    "}" +
                    "var inputs = document.querySelectorAll('input, select, textarea');" +
                    "for (var i = 0; i < inputs.length; i++) {" +
                    "  var el = inputs[i];" +
                    "  var rect = el.getBoundingClientRect();" +
                    "  if (rect.width > 0 && rect.height > 0) {" +
                    "    out.push(el.tagName + ': name=' + (el.name||'') + ' id=' + (el.id||'') + ' type=' + (el.type||'') + ' placeholder=' + (el.placeholder||'') + ' value=\"' + (el.value||'').substring(0,50) + '\"');" +
                    "  }" +
                    "}" +
                    "return out.join('\\n');");
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("[TrustRegistryPage] Diagnostic failed: " + e.getMessage());
        }
        System.out.println("[TrustRegistryPage] === END DIAGNOSTIC ===");
    }

    /**
     * Enters the DID value into the DID input field.
     */
    public void enterDID(String did) {
        System.out.println("[TrustRegistryPage] Entering DID: " + did);
        WebElement input = findVisibleInput(didInput, "DID");
        setInputValueFast(input, did);
        System.out.println("[TrustRegistryPage] DID entered: " + did);
    }

    /**
     * Enters the Aka URL into the Aka input field.
     */
    public void enterAka(String akaUrl) {
        System.out.println("[TrustRegistryPage] Entering Aka: " + akaUrl);
        dumpFormFields();
        WebElement input = findVisibleInput(akaInput, "Aka");
        setInputValueFast(input, akaUrl);
        // Verify the value was actually set
        String actualValue = normalize(input.getAttribute("value"));
        System.out.println("[TrustRegistryPage] Aka field actual value after set: \"" + actualValue + "\"");
        if (!normalize(akaUrl).equals(actualValue)) {
            System.out.println("[TrustRegistryPage] WARNING: Aka value mismatch! Expected: " + akaUrl);
        }
        System.out.println("[TrustRegistryPage] Aka entered: " + akaUrl);
    }

    /**
     * Selects "English" in the Primary Governance Framework Language field.
     * Handles native <select>, custom dropdown, and text input.
     */
    public void selectGovernanceLanguageEnglish() {
        System.out.println("[TrustRegistryPage] Setting Primary Governance Framework Language to English...");

        int implicitWaitSeconds = Integer.parseInt(
                com.verana.utils.DriverManager.getConfig().getProperty("implicit.wait.seconds", "2"));
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(0));

        try {
            // Strategy 1: Native <select>
            if (tryNativeSelectEnglish()) return;

            // Strategy 2: Custom dropdown trigger (button/combobox) -> pick English option
            if (tryCustomDropdownEnglish()) return;

            // Strategy 3: Text input - type "English"
            if (tryInputEnglish()) return;

            // Strategy 4: Broad search - find any select/dropdown near "language" label
            if (tryBroadLanguageSelect()) return;

            System.out.println("[TrustRegistryPage] WARNING: Could not find governance language field.");
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWaitSeconds));
        }
    }

    /**
     * Enters the Governance Framework Primary Document URL.
     */
    public void enterGovernanceDocUrl(String url) {
        System.out.println("[TrustRegistryPage] Entering Governance Framework Primary Document URL: " + url);
        WebElement input = findVisibleInput(governanceDocUrlInput, "Governance Doc URL");
        setInputValueFast(input, url);
        System.out.println("[TrustRegistryPage] Governance Doc URL entered: " + url);
    }

    /**
     * Clicks the Confirm/Submit/Create button to submit the ecosystem form.
     * Dumps visible buttons for debugging, then clicks the best match.
     */
    public void clickConfirm() {
        System.out.println("[TrustRegistryPage] Looking for Confirm button...");

        // Dump all visible buttons for debugging
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                    "var out = [];" +
                    "var btns = document.querySelectorAll('button, [role=\"button\"], input[type=\"submit\"]');" +
                    "for (var i = 0; i < btns.length; i++) {" +
                    "  var b = btns[i];" +
                    "  var rect = b.getBoundingClientRect();" +
                    "  if (rect.width > 0 && rect.height > 0) {" +
                    "    out.push('BUTTON: \"' + (b.innerText||b.value||'').trim().substring(0,60) + '\" disabled=' + b.disabled + ' type=' + (b.type||''));" +
                    "  }" +
                    "}" +
                    "return out.join('\\n');");
            System.out.println("[TrustRegistryPage] === VISIBLE BUTTONS ===");
            System.out.println(result);
            System.out.println("[TrustRegistryPage] === END BUTTONS ===");
        } catch (Exception ignored) {}

        // Try exact match first, then broader matches
        String[] buttonTexts = {"confirm", "submit", "create ecosystem"};
        for (String text : buttonTexts) {
            try {
                List<WebElement> buttons = driver.findElements(By.xpath(
                        "//button[not(@disabled)]"));
                for (WebElement btn : buttons) {
                    if (!btn.isDisplayed() || !btn.isEnabled()) continue;
                    String btnText = btn.getText().trim().toLowerCase();
                    if (btnText.contains(text)) {
                        scrollIntoView(btn);
                        click(btn);
                        System.out.println("[TrustRegistryPage] Clicked confirm button: '" + btn.getText().trim() + "'");
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Fallback: try type=submit
        try {
            WebElement btn = wait.waitForClickable(By.xpath("//button[@type='submit']"), 2);
            scrollIntoView(btn);
            click(btn);
            System.out.println("[TrustRegistryPage] Clicked submit button (type=submit): '" + btn.getText().trim() + "'");
            return;
        } catch (Exception ignored) {}

        throw new RuntimeException("Could not find Confirm button. Check the VISIBLE BUTTONS dump above.");
    }

    /**
     * Waits for a transaction success message after Keplr approval.
     */
    public boolean waitForTransactionSuccess(int seconds) {
        System.out.println("[TrustRegistryPage] Waiting for transaction success (up to " + seconds + "s)...");
        try {
            WebElement success = wait.waitForVisible(transactionSuccessMessage, seconds);
            System.out.println("[TrustRegistryPage] Transaction success: " + success.getText().trim());
            return true;
        } catch (Exception e) {
            System.out.println("[TrustRegistryPage] Transaction success message not found within timeout.");
            return false;
        }
    }

    // =========================================================================
    // PRIVATE helpers
    // =========================================================================

    private boolean tryNativeSelectEnglish() {
        try {
            List<WebElement> selects = driver.findElements(governanceLanguageSelect);
            for (WebElement sel : selects) {
                if (!sel.isDisplayed()) continue;
                org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(sel);
                // Try to find "English" option
                List<WebElement> opts = select.getOptions();
                for (WebElement opt : opts) {
                    if (opt.getText().trim().toLowerCase().contains("english")) {
                        select.selectByVisibleText(opt.getText().trim());
                        System.out.println("[TrustRegistryPage] Selected English via native <select>: " + opt.getText());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[TrustRegistryPage] Native <select> strategy failed: " + e.getMessage());
        }
        return false;
    }

    private boolean tryCustomDropdownEnglish() {
        return false;
    }

    private boolean tryInputEnglish() {
        return false;
    }

    private boolean tryBroadLanguageSelect() {
        try {
            // Find all selects on the page and check if any have "English" option
            List<WebElement> allSelects = driver.findElements(By.tagName("select"));
            for (WebElement sel : allSelects) {
                if (!sel.isDisplayed()) continue;
                org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(sel);
                for (WebElement opt : select.getOptions()) {
                    if (opt.getText().trim().toLowerCase().contains("english")) {
                        select.selectByVisibleText(opt.getText().trim());
                        System.out.println("[TrustRegistryPage] Selected English via broad <select> search: " + opt.getText());
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[TrustRegistryPage] Broad select strategy failed: " + e.getMessage());
        }
        return false;
    }

    private WebElement findVisibleInput(By locator, String fieldName) {
        try {
            List<WebElement> inputs = driver.findElements(locator);
            for (WebElement input : inputs) {
                if (input.isDisplayed()) {
                    scrollIntoView(input);
                    return input;
                }
            }
        } catch (Exception ignored) {}

        // Fallback: use wait
        WebElement input = wait.waitForVisible(locator, 8);
        scrollIntoView(input);
        return input;
    }

    private static final Keys SELECT_ALL_KEY =
            System.getProperty("os.name", "").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;

    private void setInputValueFast(WebElement input, String value) {
        try {
            input.click();
            input.sendKeys(Keys.chord(SELECT_ALL_KEY, "a"));
            input.sendKeys(Keys.BACK_SPACE);
            input.sendKeys(value);
            WaitUtils.sleep(80);
            String current = normalize(input.getAttribute("value"));
            if (normalize(value).equals(current)) {
                return;
            }
        } catch (Exception ignored) {}

        try {
            new Actions(driver)
                    .click(input)
                    .keyDown(SELECT_ALL_KEY).sendKeys("a").keyUp(SELECT_ALL_KEY)
                    .sendKeys(Keys.BACK_SPACE)
                    .sendKeys(value)
                    .perform();
            WaitUtils.sleep(80);
            String current = normalize(input.getAttribute("value"));
            if (normalize(value).equals(current)) {
                return;
            }
        } catch (Exception ignored) {}

        // Last fallback: JS set + events
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].focus();" +
                    "arguments[0].value = arguments[1];" +
                    "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));" +
                    "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                    input, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set value for field.", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        WaitUtils.sleep(30);
    }

    private void click(WebElement element) {
        try {
            element.click();
        } catch (Exception clickError) {
            try {
                new Actions(driver)
                        .moveToElement(element)
                        .pause(Duration.ofMillis(60))
                        .click()
                        .perform();
            } catch (Exception actionsError) {
                System.out.println("[TrustRegistryPage] Falling back to JS click.");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            }
        }
    }
}
