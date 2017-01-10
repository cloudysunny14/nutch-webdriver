nutch-webdriver
================

Nutch plugins for working with Firefox Webdriver using Selenium.
It made to work with Hadoop 2.x series pseudo-distributed and full-distributed.
Also, It made with reference to [nutch-selenium](https://github.com/momer/nutch-selenium).

This plugin allows you to fetch websites that Ajax page, while relying on the rest of the awesome Nutch stack!

## Installation (tested on Ubuntu 14.04.1)

Part 1: Setting up Selenium

A) Ensure that you have Firefox installed
```
# More info about the package @ [launchpad](https://launchpad.net/ubuntu/trusty/+source/firefox)

sudo apt-get install firefox
```
B) Install Xvfb and its associates
```
sudo apt-get install xorg synaptic xvfb gtk2-engines-pixbuf xfonts-cyrillic xfonts-100dpi \
    xfonts-75dpi xfonts-base xfonts-scalable freeglut3-dev dbus-x11 openbox x11-xserver-utils \
    libxrender1 cabextract
```

C) Install geckodriver

Download the binary of [geckodriver](https://github.com/mozilla/geckodriver/releases)

```
wget https://github.com/mozilla/geckodriver/releases/download/v0.12.0/geckodriver-v0.12.0-linux64.tar.gz
tar xvfz geckodriver-v0.12.0-linux64.tar.gz && mv geckodriver /usr/bin/
```

D) Set a display for Xvfb, so that firefox believes a display is connected
```
sudo /usr/bin/Xvfb :11 -screen 0 1024x768x24 &
sudo export DISPLAY=:11
```

E) Launch GeckoDriver
```
geckodriver -b /usr/bin/firefox -p 4444
```

Part 2: Installing plugin for Nutch (where NUTCH_HOME is the root of your nutch install)

A) Add Selenium to your Nutch dependencies
```
<!-- NUTCH_HOME/ivy/ivy.xml -->

<ivy-module version="1.0">
  <dependencies>
    ...
    <!-- begin selenium dependencies -->
    <dependency org="org.seleniumhq.selenium" name="selenium-java" rev="3.0.1" />

    <dependency org="com.opera" name="operadriver" rev="1.5">
      <exclude org="org.seleniumhq.selenium" name="selenium-remote-driver" />
    </dependency>
    <!-- end selenium dependencies -->
  </dependencies>
</ivy-module>
```
B) Add the required plugins to your `NUTCH_HOME/src/plugin/build.xml`
```
<!-- NUTCH_HOME/src/plugin/build.xml -->

<project name="Nutch" default="deploy-core" basedir=".">
  <!-- ====================================================== -->
  <!-- Build & deploy all the plugin jars.                    -->
  <!-- ====================================================== -->
  <target name="deploy">
    ... 
    <ant dir="lib-webdriver" target="deploy"/>
    <ant dir="protocol-webdriver" target="deploy" />
  </target>
      ...
</project>
```

C) Ensure that the plugin will be used as the fetcher/initial parser in your config
```
<!-- NUTCH_HOME/conf/nutch-site.xml -->

<configuration>
  ...
  <property>
    <name>plugin.includes</name>
    <value>protocol-webdriver|urlfilter-regex|parse-(html|tika)|index-(basic|anchor)|urlnormalizer-(pass|regex|basic)|scoring-opic</value>
    <description>Regular expression naming plugin directory names to
    include.  Any plugin not matching this expression is excluded.
    In any case you need at least include the nutch-extensionpoints plugin. By
    default Nutch includes crawling just HTML and plain text via HTTP,
    and basic indexing and search plugins. In order to use HTTPS please enable 
    protocol-httpclient, but be aware of possible intermittent problems with the 
    underlying commons-httpclient library.
    </description>
  </property>
```
D) Add the plugin folders to your installation's `NUTCH_HOME/src/plugin` directory

```
nutch
|-- src
|    |-- plugin
|          |-- lib-*
|          |-- protocol-*
|          ...
|          |-- lib-webdriver
|          +-- protocol-webdriver
...
```
E) Compile nutch
```
ant runtime
```

F) Start your web crawl (Ensure that you followed the above steps and have started your xvfb display as shown above)
```
NUTCH_HOME/runtime/bin/crawl $SEEDURLS webpage $NUTCH_SOLR_SERVER $NUTCH_CRAWL_DEPTH
```

