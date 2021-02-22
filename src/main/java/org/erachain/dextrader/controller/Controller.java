package org.erachain.dextrader.controller;

import org.erachain.dextrader.api.ApiClient;
import org.erachain.dextrader.settings.Settings;
import org.erachain.dextrader.traders.TradersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;


/**
 * main class for connection all modules
 */
public class Controller extends Observable {

    public static final boolean DEVELOP_USE = false;
    public static final String APP_NAME = DEVELOP_USE ? "DEX Trader TESTNET" : "DEX Trader";

    public static String version = "0.01.01";
    public static String buildTime = "2019-05-19 13:33:33 UTC";

    public static final int GENERATING_MIN_BLOCK_TIME = DEVELOP_USE ? 120 : 288; // 300 PER DAY
    public static final int GENERATING_MIN_BLOCK_TIME_MS = GENERATING_MIN_BLOCK_TIME * 1000;

    public ApiClient apiClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
    public boolean useGui = true;
    public static long buildTimestamp;
    private static Controller instance;
    private int status;
    private TradersManager tradersManager;

    private boolean isStopping = false;

    public static String getVersion() {
        return version;
    }

    public static long getBuildTimestamp() {
        if (buildTimestamp == 0) {
            Date date = new Date();
            File f = null;
            Path p = null;
            BasicFileAttributes attr = null;
            try {
                f = new File(Controller.APP_NAME.toLowerCase() + ".jar");
                p = f.toPath();
                attr = Files.readAttributes(p, BasicFileAttributes.class);
            } catch (Exception e1) {
                try {
                    f = new File(Controller.APP_NAME.toLowerCase() + ".exe");
                    p = f.toPath();
                    attr = Files.readAttributes(p, BasicFileAttributes.class);
                } catch (Exception e2) {
                }
            }

            if (attr != null) {
                buildTimestamp = attr.lastModifiedTime().toMillis();
                return buildTimestamp;
            }

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            try {
                date = formatter.parse(buildTime);
                buildTimestamp = date.getTime();
            } catch (ParseException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return buildTimestamp;
    }

    public static Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }

        return instance;
    }

    public int getStatus() {
        return this.status;
    }

    public boolean isOnStopping() {
        return this.isStopping;
    }

    public void stopAll(Integer par) {

        // PREVENT MULTIPLE CALLS
        if (this.isStopping)
            return;
        this.isStopping = true;

        if (tradersManager != null)
            tradersManager.stop();

        // FORCE CLOSE
        if (par != -999999) {
            LOGGER.info("EXIT parameter:" + par);
            System.exit(par);
            //System.
            // bat
            // if %errorlevel% neq 0 exit /b %errorlevel%
        } else {
            LOGGER.info("EXIT parameter:" + 0);
        }

    }

    public void startApplication(String args[]) {
        boolean cli = false;

        // get GRADLE bild time
        getManifestInfo();

        if (buildTimestamp == 0)
            // get local file time
            getBuildTimestamp();

        String pass = null;

        for (String arg : args) {
            if (arg.equals("-cli")) {
                cli = true;
                continue;
            }


            if (arg.equals("-nogui")) {
                useGui = false;
                continue;
            }

        }

        try {

            LOGGER.info("Starting %app%".replace("%app%", Controller.APP_NAME));
            LOGGER.info(version + " build " + buildTime);

            this.apiClient = new ApiClient();

            this.tradersManager = new TradersManager(this);

            // START
            this.status = 1;


        } catch (Exception e) {

            LOGGER.error(e.getMessage(), e);

            //USE SYSTEM STYLE
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e2) {
                LOGGER.error(e2.getMessage(), e2);
            }

            //ERROR STARTING
            LOGGER.error("STARTUP ERROR" + ": " + e.getMessage());

            //FORCE SHUTDOWN
            System.exit(0);
        }

        // CLOSE ON UNEXPECTED SHUTDOWN
        Runtime.getRuntime().addShutdownHook(new Thread(null, null, "ShutdownHook") {
            @Override
            public void run() {
                // -999999 - not use System.exit() - if freeze exit
                stopAll(-999999);
            }
        });


    }

    public static void getManifestInfo() {
        String impTitle = "Gradle Build: ERA";

        try {
            Enumeration<URL> resources = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try {
                    Manifest manifest = new Manifest(resources.nextElement().openStream());
                    Attributes attributes = manifest.getMainAttributes();
                    String implementationTitle = attributes.getValue("Implementation-Title");
                    if (implementationTitle != null && implementationTitle.equals(impTitle)) {
                        version = attributes.getValue("Implementation-Version");
                        buildTime = attributes.getValue("Build-Time");

                        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                        try {
                            Date date = formatter.parse(buildTime);
                            buildTimestamp = date.getTime();
                        } catch (ParseException e) {
                            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                            try {
                                Date date = formatter.parse(buildTime);
                                buildTimestamp = date.getTime();
                            } catch (ParseException e1) {
                                LOGGER.error(e.getMessage(), e1);
                            }
                        }
                    }
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
        }
    }
}
