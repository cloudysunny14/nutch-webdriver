/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.protocol.webdriver;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.protocol.webdriver.driver.NutchDriverService;
import org.apache.nutch.protocol.webdriver.driver.NutchFirefoxDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.service.DriverService;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * This class is used for extract content from given url using WebDriver. It
 * allows configure the marionette service port to request webdriver commands
 * via {@link org.openqa.selenium.remote.HttpCommandExecutor}
 * 
 * @author Kiyonari Harigae
 *
 */
public class HttpWebClient {

  public static String getHtmlPage(String url, Configuration conf) {
    WebDriver driver = null;

    try {
      FirefoxProfile profile = new FirefoxProfile();
      String proxyHost = conf.get("http.proxy.host");
      int proxyPort = conf.getInt("http.proxy.port", 8080);
      if (proxyHost != null && proxyHost.length() > 0) {
        profile.setPreference("network.proxy.type", 1);
        profile.setPreference("network.proxy.http", proxyHost);
        profile.setPreference("network.proxy.http_port", proxyPort);
      }

      DesiredCapabilities capabilities = DesiredCapabilities.firefox();
      capabilities.setCapability("marionette", true);
      capabilities.setCapability("firefox_profile", profile);

      int driverServicePort = conf.getInt("webdriver.service.port", 4444);
      DriverService ds = NutchDriverService
          .createDriverService(driverServicePort);
      driver = new NutchFirefoxDriver(ds, capabilities);

      driver.get(url);
      // Wait for the page to load, timeout after 3 seconds
      new WebDriverWait(driver, 3);

      // Extract body
      String innerHtml = 
          driver.findElement(By.tagName("body")).getAttribute("innerHTML");
      return innerHtml;
      // I'm sure this catch statement is a code smell ; borrowing it from
      // lib-htmlunit
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (driver != null)
        try {
          driver.quit();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
    }
  };

  public static String getHtmlPage(String url) {
    return getHtmlPage(url, null);
  }
}
