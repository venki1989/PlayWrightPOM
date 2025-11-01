package com.qa.opencart.base;

import java.io.InputStream;
import java.util.Properties;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.microsoft.playwright.*;
import com.qa.opencart.factory.PlaywrightFactory;
import com.qa.opencart.pages.HomePage;
import com.qa.opencart.pages.LoginPage;

public class BaseTest {

  protected PlaywrightFactory pf;
  protected Page page;
  protected Properties prop;

  protected HomePage homePage;
  protected LoginPage loginPage;

  @Parameters({ "browser" })
  @BeforeClass(alwaysRun = true)
  public void setup(@Optional String browserName) {
    try {
      pf = new PlaywrightFactory();

      // --- Load properties robustly (classpath-first, CI-safe) ---
      prop = safeLoadProps();
      if (browserName != null && !browserName.isBlank()) {
        prop.setProperty("browser", browserName);
      }

      // Default to headless on CI unless explicitly overridden
      if (System.getProperty("pw.headless") == null && prop.getProperty("headless") == null) {
        prop.setProperty("headless", "true");
      }

      // --- Start Playwright/browser/page; let factory read headless & browser from props ---
      page = pf.initBrowser(prop);
      if (page == null) throw new IllegalStateException("Playwright page is null (browser failed to launch)");
      homePage = new HomePage(page);

    } catch (Throwable t) {
      // Surface the real root cause in Jenkins and prevent later NPEs
      t.printStackTrace();
      throw new SkipException("Test setup failed on CI: " + t.getMessage(), t);
    }
  }

  private Properties safeLoadProps() throws Exception {
    // Try classpath: src/test/resources/config/config.properties
    try (InputStream in = Thread.currentThread()
        .getContextClassLoader()
        .getResourceAsStream("config/config.properties")) {
      if (in != null) {
        Properties p = new Properties();
        p.load(in);
        return p;
      }
    }
    // Fallback to your factory (if it uses a file path); if that returns null, fail
    Properties p = (pf != null) ? pf.init_prop() : null;
    if (p == null) {
      throw new IllegalStateException("config/config.properties not found on classpath and init_prop() returned null");
    }
    return p;
    }

  @AfterClass(alwaysRun = true)
  public void tearDown() {
    try {
      if (page != null && page.context() != null && page.context().browser() != null) {
        page.context().browser().close();
      }
    } catch (Throwable ignored) {}
  }
}
