package org.erachain.eratrader.controller;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.erachain.api.ApiClient;
import org.erachain.api.ApiService;
import org.erachain.at.AT;
import org.erachain.core.*;
import org.erachain.core.BlockGenerator.ForgingStatus;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.crypto.AEScrypto;
import org.erachain.core.crypto.Base32;
import org.erachain.core.crypto.Base58;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.Order;
import org.erachain.core.item.assets.Trade;
import org.erachain.core.item.imprints.ImprintCls;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.item.persons.PersonHuman;
import org.erachain.core.item.polls.PollCls;
import org.erachain.core.item.statuses.StatusCls;
import org.erachain.core.item.templates.TemplateCls;
import org.erachain.core.item.unions.UnionCls;
import org.erachain.core.naming.Name;
import org.erachain.core.naming.NameSale;
import org.erachain.core.payment.Payment;
import org.erachain.core.telegram.TelegramStore;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.transaction.TransactionFactory;
import org.erachain.core.voting.PollOption;
import org.erachain.core.wallet.Wallet;
import org.erachain.database.DLSet;
import org.erachain.database.SortableList;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.ItemMap;
import org.erachain.datachain.LocalDataMap;
import org.erachain.datachain.TransactionMap;
import org.erachain.eratrader.traders.TradersManager;
import org.erachain.gui.AboutFrame;
import org.erachain.gui.Gui;
import org.erachain.gui.GuiTimer;
import org.erachain.gui.library.IssueConfirmDialog;
import org.erachain.lang.Lang;
import org.erachain.network.Network;
import org.erachain.network.Peer;
import org.erachain.network.message.*;
import org.erachain.ntp.NTP;
import org.erachain.settings.Settings;
import org.erachain.traders.TradersManager;
import org.erachain.utils.*;
import org.erachain.webserver.Status;
import org.erachain.webserver.WebService;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.mapdb.Fun.Tuple5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.swing.*;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import java.util.concurrent.ConcurrentHashMap;
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



    public int getStatus() {
        return this.status;
    }

    public void start() throws Exception {


        guiTimer = new GuiTimer();

        this.setChanged();
        this.notifyObservers(new ObserverMessage(ObserverMessage.GUI_ABOUT_TYPE, Lang.getInstance().translate("Wallet OK")));

        if (Settings.getInstance().isTestnet() && this.wallet.isWalletDatabaseExisting()
                && !this.wallet.getAccounts().isEmpty()) {
            this.wallet.synchronize(true);
        }
        // create telegtam


        // CLOSE ON UNEXPECTED SHUTDOWN
        Runtime.getRuntime().addShutdownHook(new Thread(null, null, "ShutdownHook") {
            @Override
            public void run() {
                // -999999 - not use System.exit() - if freeze exit
                stopAll(-999999);
                //Runtime.getRuntime().removeShutdownHook(currentThread());
            }
        });

        if (Settings.getInstance().isTestnet())
            this.status = STATUS_OK;

        // REGISTER DATABASE OBSERVER
        // this.addObserver(this.DLSet.getPeerMap());
        this.addObserver(this.dcSet);

        // CREATE NETWORK
        this.tradersManager = new TradersManager();

        updateCompuRaes();
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

        this.setChanged();
        this.notifyObservers(new ObserverMessage(ObserverMessage.GUI_ABOUT_TYPE, Lang.getInstance().translate("Closing")));
        // STOP MESSAGE PROCESSOR
        this.setChanged();
        this.notifyObservers(new ObserverMessage(ObserverMessage.GUI_ABOUT_TYPE, Lang.getInstance().translate("Stopping message processor")));

    }



    /**
     * Total time in disconnect
     *
     * @return
     */
    public long getToOfflineTime() {
        return this.toOfflineTime;
    }

    public void setToOfflineTime(long time) {
        this.toOfflineTime = time;
    }


    public List<byte[]> getNextHeaders(byte[] signature) {
        return this.blockChain.getSignatures(dcSet, signature);
    }

    public void setOrphanTo(int height) {
        this.blockChain.clearWaitWinBuffer();
        this.blockGenerator.setOrphanTo(height);
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

        if (useGui) {

            this.about_frame = AboutFrame.getInstance();
            this.addSingleObserver(about_frame);
            this.about_frame.setUserClose(false);
            this.about_frame.setModal(false);
            this.about_frame.setVisible(true);
        }
        if (!cli) {
            try {

                //ONE MUST BE ENABLED
                if (!Settings.getInstance().isGuiEnabled() && !Settings.getInstance().isRpcEnabled()) {
                    throw new Exception(Lang.getInstance().translate("Both gui and rpc cannot be disabled!"));
                }

                LOGGER.info(Lang.getInstance().translate("Starting %app%")
                        .replace("%app%", Lang.getInstance().translate(Controller.APP_NAME)));
                LOGGER.info(version + Lang.getInstance().translate(" build ")
                        + buildTime);

                this.setChanged();
                this.notifyObservers(new ObserverMessage(ObserverMessage.GUI_ABOUT_TYPE, info));


                String licenseFile = "Erachain Licence Agreement (genesis).txt";
                File f = new File(licenseFile);
                if (!f.exists()) {

                    LOGGER.error("License file not found: " + licenseFile);

                    //FORCE SHUTDOWN
                    System.exit(3);

                }


                //STARTING NETWORK/BLOCKCHAIN/RPC

                Controller.getInstance().start();

                //unlock wallet

                if (pass != null && doesWalletDatabaseExists()) {
                    if (unlockWallet(pass))
                        lockWallet();
                }

                Status.getinstance();

                if (!useGui) {
                    LOGGER.info("-nogui used");
                } else {

                    try {
                        Thread.sleep(100);

                        //START GUI

                        gui = Gui.getInstance();

                        if (gui != null && Settings.getInstance().isSysTrayEnabled()) {

                            SysTray.getInstance().createTrayIcon();
                            about_frame.setVisible(false);
                        }
                    } catch (Exception e1) {
                        if (about_frame != null) {
                            about_frame.setVisible(false);
                            about_frame.dispose();
                        }
                        LOGGER.error(Lang.getInstance().translate("GUI ERROR - at Start"), e1);
                    }
                }

                if (Controller.getInstance().doesWalletExists()) {
                    Controller.getInstance().wallet.initiateItemsFavorites();
                }


            } catch (Exception e) {

                LOGGER.error(e.getMessage(), e);
                // show error dialog
                if (useGui) {
                    if (Settings.getInstance().isGuiEnabled()) {
                        IssueConfirmDialog dd = new IssueConfirmDialog(null, true, null,
                                Lang.getInstance().translate("STARTUP ERROR") + ": " + e.getMessage(), 600, 400, Lang.getInstance().translate(" "));
                        dd.jButton1.setVisible(false);
                        dd.jButton2.setText(Lang.getInstance().translate("Cancel"));
                        dd.setLocationRelativeTo(null);
                        dd.setVisible(true);
                    }
                }

                //USE SYSTEM STYLE
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e2) {
                    LOGGER.error(e2.getMessage(), e2);
                }

                //ERROR STARTING
                LOGGER.error(Lang.getInstance().translate("STARTUP ERROR") + ": " + e.getMessage());

                if (Gui.isGuiStarted()) {
                    JOptionPane.showMessageDialog(null, e.getMessage(), Lang.getInstance().translate("Startup Error"), JOptionPane.ERROR_MESSAGE);

                }


                if (about_frame != null) {
                    about_frame.setVisible(false);
                    about_frame.dispose();
                }
                //FORCE SHUTDOWN
                System.exit(0);
            }
        } else {
            Scanner scanner = new Scanner(System.in);
            ApiClient client = new ApiClient();

            while (true) {

                System.out.print("[COMMAND] ");
                String command = scanner.nextLine();

                if (command.equals("quit")) {

                    if (about_frame != null) {
                        about_frame.setVisible(false);
                        about_frame.dispose();
                    }
                    scanner.close();
                    System.exit(0);
                }

                String result = client.executeCommand(command);
                LOGGER.info("[RESULT] " + result);
            }
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
