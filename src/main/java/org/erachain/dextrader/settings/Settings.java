package org.erachain.dextrader.settings;
// 17/03 Qj1vEeuz7iJADzV2qrxguSFGzamZiYZVUP
// 30/03 ++

import java.io.File;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.erachain.dextrader.controller.Controller;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

//import java.util.Arrays;
// import org.slf4j.LoggerFactory;

public class Settings {
    //private static final String[] DEFAULT_PEERS = { };
    public static final String DEFAULT_THEME = "System";

    public static final int DEFAULT_WEB_PORT = Controller.DEVELOP_USE ? 9067 : 9047;
    public static final int DEFAULT_RPC_PORT = Controller.DEVELOP_USE ? 9068 : 9048;

    public static final String DEFAULT_DATA_DIR = "datachain";

    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    private static final Integer DEFAULT_FONT_SIZE = 11;
    private static final String DEFAULT_FONT_NAME = "Arial";

    // NETWORK
    private static final int DEFAULT_CONNECTION_TIMEOUT = 20000;
    private static final int DEFAULT_PING_INTERVAL = Controller.GENERATING_MIN_BLOCK_TIME_MS;

    //RPC
    private static final String DEFAULT_RPC_ALLOWED = "127.0.0.1"; // localhost = error in accessHandler.setWhite(Settings.getInstance().getRpcAllowed());
    private static final boolean DEFAULT_RPC_ENABLED = false;
    private static final boolean DEFAULT_BACUP_ENABLED = false;
    private static final boolean DEFAULT_BACKUP_ASK_ENABLED = false;

    //GUI CONSOLE
    private static final boolean DEFAULT_GUI_CONSOLE_ENABLED = true;

    //WEB
    private static final String DEFAULT_WEB_ALLOWED = "127.0.0.1";
    private static final boolean DEFAULT_WEB_ENABLED = true;

    // 19 03
    //GUI
    private static final boolean DEFAULT_GUI_ENABLED = true;
    private static final boolean DEFAULT_GUI_DYNAMIC = true;
    private static final boolean DEFAULT_GENERATOR_KEY_CACHING = true;
    private static final boolean DEFAULT_CHECKPOINTING = true;
    private static final boolean DEFAULT_SOUND_RECEIVE_COIN = true;
    private static final boolean DEFAULT_SOUND_MESSAGE = true;
    private static final boolean DEFAULT_SOUND_NEW_TRANSACTION = true;
    //private static final int DEFAULT_MAX_BYTE_PER_FEE = 512;
    private static final boolean ALLOW_FEE_LESS_REQUIRED = false;
    //DATE FORMAT
    private static final String DEFAULT_TIME_ZONE = ""; //"GMT+3";

    //private static final BigDecimal DEFAULT_BIG_FEE = new BigDecimal(1000);
    private static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";
    private static final String DEFAULT_BIRTH_TIME_FORMAT = "yyyy-MM-dd HH:mm z";
    private static final boolean DEFAULT_NS_UPDATE = false;
    private static final boolean DEFAULT_FORGING_ENABLED = true;

    /**
     * нельзя тут использовать localhost - только 127....
     * ex: http://127.0.0.1/7pay_in/tools/block_proc/ERA
     */
    private static final String NOTIFY_INCOMING_URL = "http://127.0.0.1:8000/exhange/era/income";
    private static final int NOTIFY_INCOMING_CONFIRMATIONS = 0;
    public static String DEFAULT_LANGUAGE = "en";

    public static final boolean USE_TELEGRAM_STORE = false;
    public static final int TELEGRAM_STORE_PERIOD = 5; // in days



    private static Settings instance;
    private JSONObject settingsJSON;
    public  JSONArray tradersJSON;
    public  JSONObject apiKeysJSON;
    private JSONObject peersJSON;
    private String userPath = "";
    private InetAddress localAddress;

    private String dataPath;

    private Settings() {

        readAPIkeysJSON();

        this.localAddress = this.getCurrentIp();
        settingsJSON = read_setting_JSON();

        File file = new File("");
        //TRY READ PEERS.JSON
        try {
            //OPEN FILE
            file = new File(this.getPeersPath());

            //CREATE FILE IF IT DOESNT EXIST
            if (file.exists()) {
                //READ PEERS FILE
                List<String> lines = Files.readLines(file, Charsets.UTF_8);

                String jsonString = "";
                for (String line : lines) {
                    jsonString += line;
                }

                //CREATE JSON OBJECT
                this.peersJSON = (JSONObject) JSONValue.parse(jsonString);
            } else {
                this.peersJSON = new JSONObject();
            }

        } catch (Exception e) {
            LOGGER.info("Error while reading peers.json " + file.getAbsolutePath() + " using default!");
            LOGGER.error(e.getMessage(), e);
            this.peersJSON = new JSONObject();
        }
    }

    public static Settings getInstance() {
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            if (instance == null) {

                instance = new Settings();
            }
        } finally {
            lock.unlock();
        }
        return instance;
    }

    public static void FreeInstance() {
        if (instance != null) {
            instance = null;
        }
    }

    public JSONObject Dump() {
        return (JSONObject) settingsJSON.clone();
    }

    public String getSettingsPath() {
        return this.userPath + "settings.json";
    }

    public String getGuiSettingPath() {

        return this.userPath + "gui_settings.json";

    }

    public String getPeersPath() {
        return this.userPath + (Controller.DEVELOP_USE ? "peers-dev.json" : "peers.json");
    }

    public String getDataDir() {
        try {
			if (this.dataPath.equals(""))
			    return this.getUserPath() + DEFAULT_DATA_DIR;
			return this.dataPath;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return this.getUserPath() + DEFAULT_DATA_DIR;
		}
    }

    public String getLangDir() {
        return this.getUserPath() + "languages";
    }

    public String getUserPath() {
        return this.userPath;
    }

    // http://127.0.0.1:8000/ipay3_free/tools/block_proc/ERA
    public String getNotifyIncomingURL() {
        if (this.settingsJSON.containsKey("notify_incoming_url")) {
            return (String) this.settingsJSON.get("notify_incoming_url");
        }
        return NOTIFY_INCOMING_URL;
    }

    public int getNotifyIncomingConfirmations() {
        if (this.settingsJSON.containsKey("notify_incoming_confirmations")) {
            return (int) (long) this.settingsJSON.get("notify_incoming_confirmations");
        }

        return NOTIFY_INCOMING_CONFIRMATIONS;
    }

    public JSONArray getPeersJson() {
        if (this.peersJSON != null && this.peersJSON.containsKey("knownpeers")) {
            return (JSONArray) this.peersJSON.get("knownpeers");
        } else {
            return new JSONArray();
        }

    }


    public int getRpcPort() {
        if (this.settingsJSON.containsKey("rpcport")) {
            return ((Long) this.settingsJSON.get("rpcport")).intValue();
        }

        return DEFAULT_RPC_PORT;
    }

    public String[] getRpcAllowed() {
        try {
            if (this.settingsJSON.containsKey("rpcallowed")) {
                //GET PEERS FROM JSON
                JSONArray allowedArray = (JSONArray) this.settingsJSON.get("rpcallowed");

                //CREATE LIST WITH PEERS
                String[] allowed = new String[allowedArray.size()];
                for (int i = 0; i < allowedArray.size(); i++) {
                    allowed[i] = (String) allowedArray.get(i);
                }

                //RETURN
                return allowed;
            }

            //RETURN
            return DEFAULT_RPC_ALLOWED.split(";");
        } catch (Exception e) {
            //RETURN EMPTY LIST
            return new String[0];
        }
    }

    public boolean isRpcEnabled() {
        if (this.settingsJSON.containsKey("rpcenabled")) {
            return ((Boolean) this.settingsJSON.get("rpcenabled")).booleanValue();
        }

        return DEFAULT_RPC_ENABLED;
    }

    public boolean getbacUpEnabled() {
        if (this.settingsJSON.containsKey("backupenabled")) {
            return ((Boolean) this.settingsJSON.get("backupenabled")).booleanValue();
        }

        return DEFAULT_BACUP_ENABLED;
    }

    public String getCompuRate() {
        if (this.settingsJSON.containsKey("compuRate")) {
            return ((String) this.settingsJSON.get("compuRate")).toString();
        }

        return "100";
    }


    public int getPingInterval() {
        if (this.settingsJSON.containsKey("pinginterval")) {
            return ((Long) this.settingsJSON.get("pinginterval")).intValue();
        }

        return DEFAULT_PING_INTERVAL;
    }

    public boolean getbacUpAskToStart() {
        if (this.settingsJSON.containsKey("backupasktostart")) {
            return ((Boolean) this.settingsJSON.get("backupasktostart")).booleanValue();
        }

        return DEFAULT_BACKUP_ASK_ENABLED;
    }

    public int getWebPort() {
        if (this.settingsJSON.containsKey("webport")) {
            return ((Long) this.settingsJSON.get("webport")).intValue();
        }

        return DEFAULT_WEB_PORT;
    }

    public boolean isGuiConsoleEnabled() {
        if (this.settingsJSON.containsKey("guiconsoleenabled")) {
            return ((Boolean) this.settingsJSON.get("guiconsoleenabled")).booleanValue();
        }

        return DEFAULT_GUI_CONSOLE_ENABLED;
    }

    public boolean isSoundReceivePaymentEnabled() {
        if (this.settingsJSON.containsKey("soundreceivepayment")) {
            return ((Boolean) this.settingsJSON.get("soundreceivepayment")).booleanValue();
        }

        return DEFAULT_SOUND_RECEIVE_COIN;
    }

    public boolean isSoundReceiveMessageEnabled() {
        if (this.settingsJSON.containsKey("soundreceivemessage")) {
            return ((Boolean) this.settingsJSON.get("soundreceivemessage")).booleanValue();
        }

        return DEFAULT_SOUND_MESSAGE;
    }

    public boolean isSoundNewTransactionEnabled() {
        if (this.settingsJSON.containsKey("soundnewtransaction")) {
            return ((Boolean) this.settingsJSON.get("soundnewtransaction")).booleanValue();
        }

        return DEFAULT_SOUND_NEW_TRANSACTION;
    }
	

    public boolean isGuiEnabled() {

        if (System.getProperty("nogui") != null) {
            return false;
        }
        if (this.settingsJSON.containsKey("guienabled")) {
            return ((Boolean) this.settingsJSON.get("guienabled")).booleanValue();
        }

        return DEFAULT_GUI_ENABLED;
    }

    public boolean isGuiDynamic() {
        if (this.settingsJSON.containsKey("guidynamic")) {
            return ((Boolean) this.settingsJSON.get("guidynamic")).booleanValue();
        }

        return DEFAULT_GUI_DYNAMIC;
    }

    public String getTimeZone() {
        if (this.settingsJSON.containsKey("timezone")) {
            return (String) this.settingsJSON.get("timezone");
        }

        return DEFAULT_TIME_ZONE;
    }

    public String getTimeFormat() {
        if (this.settingsJSON.containsKey("timeformat")) {
            return (String) this.settingsJSON.get("timeformat");
        }

        return DEFAULT_TIME_FORMAT;
    }

    // birth
    public String getBirthTimeFormat() {
        if (this.settingsJSON.containsKey("birthTimeformat")) {
            return (String) this.settingsJSON.get("birthTimeformat");
        }

        return DEFAULT_BIRTH_TIME_FORMAT;
    }


    public boolean isSysTrayEnabled() {
        if (this.settingsJSON.containsKey("systray")) {
            return ((Boolean) this.settingsJSON.get("systray")).booleanValue();
        }
        return true;
    }

    public boolean isLocalAddress(InetAddress address) {
        try {
            if (this.localAddress == null) {
                return false;
            } else {
                return address.equals(this.localAddress);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public InetAddress getCurrentIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces
                        .nextElement();
                Enumeration<InetAddress> nias = ni.getInetAddresses();
                while (nias.hasMoreElements()) {
                    InetAddress ia = (InetAddress) nias.nextElement();
                    if (!ia.isLinkLocalAddress()
                            && !ia.isLoopbackAddress()
                            && ia instanceof Inet4Address) {
                        return ia;
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.info("unable to get current IP " + e.getMessage());
        }
        return null;
    }

    public String getLang() {
        if (this.settingsJSON.containsKey("lang")) {
            String langStr = (String)this.settingsJSON.get("lang");
            
            // delete .json
            String[] tokens = langStr.split("\\.");
            if (tokens.length > 1)
        	return tokens[0];
            return langStr;
        }

        return DEFAULT_LANGUAGE;
    }

    public String getLangFileName() {
	return getLang() + ".json";
    }

    public String get_Font() {
        if (this.settingsJSON.containsKey("font_size")) {
            return ((String) this.settingsJSON.get("font_size").toString());
        }

        return DEFAULT_FONT_SIZE.toString();

    }

    public String get_File_Chooser_Paht() {
        if (this.settingsJSON.containsKey("FileChooser_Path")) {
            return ((String) this.settingsJSON.get("FileChooser_Path").toString());
        }

        return getUserPath();

    }

    public int get_File_Chooser_Wight() {
        if (this.settingsJSON.containsKey("FileChooser_Wight")) {
            return new Integer(this.settingsJSON.get("FileChooser_Wight").toString());
        }

        return 0;

    }

    public int get_File_Chooser_Height() {
        if (this.settingsJSON.containsKey("FileChooser_Height")) {
            return new Integer(this.settingsJSON.get("FileChooser_Height").toString());
        }

        return 0;

    }

    public String get_Font_Name() {
        if (this.settingsJSON.containsKey("font_name")) {
            return ((String) this.settingsJSON.get("font_name").toString());
        }

        return DEFAULT_FONT_NAME;
    }

    public String get_Theme() {

        if (this.settingsJSON.containsKey("theme")) {
            return ((String) this.settingsJSON.get("theme").toString());
        }

        return DEFAULT_THEME;
    }

    public String get_LookAndFell() {

        if (this.settingsJSON.containsKey("LookAndFell")) {
            return ((String) this.settingsJSON.get("LookAndFell").toString());
        }

        return DEFAULT_THEME;


    }
    
   

    public String cutPath(String path) {

        //if (!(this.userPath.endsWith("\\")
        if (path.endsWith("/")) {
            path.substring(0, path.length() - 1);
            path += File.separator;
        }

        return path;


    }

    public JSONObject read_setting_JSON() {
        int alreadyPassed = 0;
        File file = new File(this.userPath + "settings.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
                JSONObject json = new JSONObject();
                json.put("!!!ver", "3.0");
                return json;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                LOGGER.error(e.getMessage(), e);
            }
        }
        try {
            while (alreadyPassed < 2) {
                //OPEN FILE
                //READ SETTINS JSON FILE
                List<String> lines = Files.readLines(file, Charsets.UTF_8);

                String jsonString = "";
                for (String line : lines) {

                    //correcting single backslash bug
                    if (line.contains("userpath")) {
                        line = line.replace("\\", File.separator);
                    }

                    jsonString += line;
                }

                //CREATE JSON OBJECT
                this.settingsJSON = (JSONObject) JSONValue.parse(jsonString);
                settingsJSON = settingsJSON == null ? new JSONObject() : settingsJSON;


                alreadyPassed++;

                if (this.settingsJSON.containsKey("userpath")) {
                    this.userPath = (String) this.settingsJSON.get("userpath");
                } else {
                    alreadyPassed++;
                }

                // set data dir getDataPath
                if (this.settingsJSON.containsKey("datadir")) {
                    this.dataPath = (String) this.settingsJSON.get("datadir");

                    try {
                        if ( false && !(this.dataPath.endsWith("\\") || this.dataPath.endsWith("/") || this.dataPath.endsWith(File.separator))) {
                            this.dataPath += File.separator;
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        this.dataPath = "";
                    }
                } else {
                    this.dataPath = "";
                }

                //CREATE FILE IF IT DOESNT EXIST
                if (!file.exists()) {
                    file.createNewFile();
                }
            }

        } catch (Exception e) {
            LOGGER.info("Error while reading/creating settings.json " + file.getAbsolutePath() + " using default!");
            LOGGER.error(e.getMessage(), e);
            settingsJSON = new JSONObject();
        }

        return settingsJSON;

    }
    
    public JSONObject getJSONObject(){
        return this.settingsJSON;
    }

    ////////////////////////////////
    public List<JSONObject> readTradersJSON() {

        File file = new File(this.userPath + "traders.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
                return null;
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        try {
            //OPEN FILE
            //READ SETTINS JSON FILE
            List<String> lines = Files.readLines(file, Charsets.UTF_8);

            String jsonString = "";
            for (String line : lines) {
                if ((line = line.trim()).startsWith("//") || line.isEmpty()) {
                    // пропускаем //
                    continue;
                }
                jsonString += line;
            }

            //CREATE JSON OBJECT
            tradersJSON = (JSONArray) JSONValue.parse(jsonString);

        } catch (Exception e) {
            LOGGER.info("Error while reading/creating traders.json " + file.getAbsolutePath() + " using default!");
            LOGGER.error(e.getMessage(), e);
        }

        return tradersJSON;

    }

    ////////////////////////////////
    public void readAPIkeysJSON() {

        File file = new File(this.userPath + "secret-keys.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                LOGGER.error(e.getMessage(), e);
            }
        }

        try {
            //OPEN FILE
            //READ SETTINS JSON FILE
            List<String> lines = Files.readLines(file, Charsets.UTF_8);

            String jsonString = "";
            for (String line : lines) {
                if ((line = line.trim()).startsWith("//") || line.isEmpty()) {
                    // пропускаем //
                    continue;
                }
                jsonString += line;
            }

            //CREATE JSON OBJECT
            apiKeysJSON = (JSONObject) JSONValue.parse(jsonString);

        } catch (Exception e) {
            LOGGER.info("Error while reading/creating secret-keys.json " + file.getAbsolutePath() + " using default!");
            LOGGER.error(e.getMessage(), e);
        }

    }

}
