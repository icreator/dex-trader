package org.erachain.eratrader.controller;

import javafx.util.Pair;
import org.erachain.eratrader.traders.TradersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

// 04/01 +-

/**
 * main class for connection all modules
 */
public class Controller extends Observable {

    public static final boolean DEVELOP_USE = true;
    public static final String APP_NAME = DEVELOP_USE ? "Erachain-dev" : "Erachain";

    public static String version = "0.01.01";
    public static String buildTime = "2019-05-19 13:33:33 UTC";

    public static final int GENERATING_MIN_BLOCK_TIME = DEVELOP_USE ? 120 : 288; // 300 PER DAY
    public static final int GENERATING_MIN_BLOCK_TIME_MS = GENERATING_MIN_BLOCK_TIME * 1000;

    public static final char DECIMAL_SEPARATOR = '.';
    public static final char GROUPING_SEPARATOR = '`';

    public static TreeMap<String, Pair<BigDecimal, String>> COMPU_RATES = new TreeMap();

    // TODO ENUM would be better here
    public static final int STATUS_NO_CONNECTIONS = 0;
    public static final int STATUS_SYNCHRONIZING = 1;
    public static final int STATUS_OK = 2;
    private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);
    public boolean useGui = true;
    private List<Thread> threads = new ArrayList<Thread>();
    public static long buildTimestamp;
    private static Controller instance;
    private int status;
    private boolean dcSetWithObserver = false;
    private boolean dynamicGUI = false;
    private TradersManager tradersManager;
    private Timer connectTimer;
    private Random random = new SecureRandom();

    private boolean isStopping = false;
    private String info;


    public static String getVersion() {
        return version;
    }

    public static long getBuildTimestamp() {
        if (buildTimestamp == 0) {
            Date date = new Date();
            //// URL resource =
            //// getClass().getResource(getClass().getSimpleName() + ".class");
            // URL resource =
            //// Controller.class.getResource(Controller.class.getSimpleName() +
            //// ".class");
            // if (resource != null && resource.getProtocol().equals("file")) {
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

    public List<String> getAccounts() {
        return null;
    }

    public int getStatus() {
        return this.status;
    }

    public void start() throws Exception {


        // CLOSE ON UNEXPECTED SHUTDOWN
        Runtime.getRuntime().addShutdownHook(new Thread(null, null, "ShutdownHook") {
            @Override
            public void run() {
                // -999999 - not use System.exit() - if freeze exit
                stopAll(-999999);
                //Runtime.getRuntime().removeShutdownHook(currentThread());
            }
        });

        // CREATE NETWORK
        this.tradersManager = new TradersManager();

    }


    public boolean isOnStopping() {
        return this.isStopping;
    }

    public void stopAll(Integer par) {
        // PREVENT MULTIPLE CALLS
        if (this.isStopping)
            return;
        this.isStopping = true;

        if (this.connectTimer != null)
            this.connectTimer.cancel();

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

            String licenseFile = "Erachain Licence Agreement (genesis).txt";
            File f = new File(licenseFile);
            if (!f.exists()) {

                LOGGER.error("License file not found: " + licenseFile);

                //FORCE SHUTDOWN
                System.exit(3);

            }


            //STARTING NETWORK/BLOCKCHAIN/RPC

            start();

            if (!useGui) {
                LOGGER.info("-nogui used");
            } else {

            }


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
