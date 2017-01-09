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
package org.apache.nutch.protocol.webdriver.driver;

import static org.openqa.selenium.firefox.FirefoxOptions.FIREFOX_OPTIONS;
import static org.openqa.selenium.remote.CapabilityType.PROXY;

import java.io.IOException;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.internal.Killable;
import org.openqa.selenium.remote.BeanToJsonConverter;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.service.DriverCommandExecutor;
import org.openqa.selenium.remote.service.DriverService;

/**
 * A FirefoxDriver without launch firefox process by myself, but instead using
 * the external process that already started in configured port.
 * 
 * @author Kiyonari Harigae
 *
 */
public class NutchFirefoxDriver extends RemoteWebDriver implements Killable {

  public static final String PROFILE = "firefox_profile";
  public final static String OLD_FIREFOX_OPTIONS = "firefoxOptions";

  public NutchFirefoxDriver(DriverService driverService,
      Capabilities desiredCapabilities) {
    this(new DriverCommandExecutor(driverService),
        firefoxCapabilities(desiredCapabilities));
  }

  public NutchFirefoxDriver(CommandExecutor executor, Capabilities capabilities) {
    super(executor, capabilities, null);
  }

  private static Capabilities firefoxCapabilities(Capabilities capabilities) {
    if (capabilities == null) {
      return new DesiredCapabilities();
    }
    FirefoxProfile profile = getProfile(capabilities);
    populateProfile(profile, capabilities);

    DesiredCapabilities caps = new DesiredCapabilities(capabilities);
    // Ensure that the proxy is in a state fit to be sent to the extension
    Proxy proxy = Proxy.extractFrom(capabilities);
    if (proxy != null) {
      caps.setCapability(PROXY, new BeanToJsonConverter().convert(proxy));
    }
    return caps;
  }

  private static FirefoxProfile getProfile(Capabilities cap) {
    FirefoxProfile profile = null;
    Object raw = null;
    if (cap != null && cap.getCapability(PROFILE) != null) {
      raw = cap.getCapability(PROFILE);
    }
    if (raw != null) {
      if (raw instanceof FirefoxProfile) {
        profile = (FirefoxProfile) raw;
      } else if (raw instanceof String) {
        try {
          profile = FirefoxProfile.fromJson((String) raw);
        } catch (IOException e) {
          throw new WebDriverException(e);
        }
      }
    }
    if (profile == null) {
      profile = new FirefoxProfile();
    }
    return profile;
  }

  static Capabilities populateProfile(FirefoxProfile profile,
      Capabilities capabilities) {
    if (capabilities == null) {
      return capabilities;
    }

    Object rawOptions = capabilities.getCapability(FIREFOX_OPTIONS);
    if (rawOptions == null) {
      rawOptions = capabilities.getCapability(OLD_FIREFOX_OPTIONS);
    }
    if (rawOptions != null && !(rawOptions instanceof FirefoxOptions)) {
      throw new WebDriverException(
          "Firefox option was set, but is not a FirefoxOption: " + rawOptions);
    }
    FirefoxOptions options = (FirefoxOptions) rawOptions;
    if (options == null) {
      options = new FirefoxOptions();
    }
    options.setProfile(profile);

    DesiredCapabilities toReturn = capabilities instanceof DesiredCapabilities ? (DesiredCapabilities) capabilities
        : new DesiredCapabilities(capabilities);
    toReturn.setCapability(OLD_FIREFOX_OPTIONS, options);
    toReturn.setCapability(FIREFOX_OPTIONS, options);
    return toReturn;
  }

  @Override
  public void kill() {
  }

}
