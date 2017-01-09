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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.remote.service.DriverService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A driverService check the GeckoDriver process that used in this session is
 * already running. Manages the session life cycle by super class of
 * {@link DriverService}
 * 
 * @author Kiyonari Harigae
 *
 */
public class NutchDriverService extends DriverService {

  public static final String GECKO_DRIVER_EXE_PROPERTY = "webdriver.gecko.driver";

  private final ReentrantLock lock = new ReentrantLock();
  private boolean isRunning;

  @Override
  public boolean isRunning() {
    lock.lock();
    try {
      return isRunning;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void start() throws IOException {
    lock.lock();
    try {
      // TODO: Check port up
      PortProber.waitForPortUp(getUrl().getPort(), 20, SECONDS);
      isRunning = true;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void stop() {
    lock.lock();
    try {
      isRunning = false;
    } finally {
      lock.unlock();
    }
  }

  protected NutchDriverService(File executable, int port,
      ImmutableList<String> args, ImmutableMap<String, String> environment)
      throws IOException {
    super(executable, port, args, environment);
  }

  public static DriverService createDriverService(int port) {
    try {
      return new NutchDriverService(findDefaultExecutable(), port, null, null);
    } catch (IOException e) {
      throw new WebDriverException(e);
    }
  }

  private static File findDefaultExecutable() {
    return findExecutable("geckodriver", GECKO_DRIVER_EXE_PROPERTY,
        "https://github.com/mozilla/geckodriver",
        "https://github.com/mozilla/geckodriver/releases");
  }
}
