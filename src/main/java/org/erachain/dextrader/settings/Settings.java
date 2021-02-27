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
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;


public class Settings {

    public static final int DEFAULT_WEB_PORT = Controller.DEVELOP_USE ? 9067 : 9047;
    public static final int DEFAULT_RPC_PORT = Controller.DEVELOP_USE ? 9068 : 9048;

    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    private static Settings instance;
    public JSONObject settingsJSON;
    public  JSONArray tradersJSON;
    public  JSONArray ratersJSON;
    public  JSONObject apiKeysJSON;

    private Settings() {

        settingsJSON = read_setting_JSON();

        readAPIkeysJSON();
        readTradersJSON();
        readRatersJSON();

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

    public int getRpcPort() {
        if (this.settingsJSON.containsKey("rpcport")) {
            return ((Long) this.settingsJSON.get("rpcport")).intValue();
        }

        return DEFAULT_RPC_PORT;
    }

    public JSONObject read_setting_JSON() {

        File file = new File("settings.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
                JSONObject json = new JSONObject();
                return json;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                LOGGER.error(e.getMessage(), e);
            }
        }

        try {
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

        File file = new File(getRpcPort() == 9068? "traders-demo.json" : "traders.json");
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

    public List<JSONObject> readRatersJSON() {

        File file = new File(getRpcPort() == 9068? "raters-demo.json" : "raters.json");
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
            ratersJSON = (JSONArray) JSONValue.parse(jsonString);

        } catch (Exception e) {
            LOGGER.info("Error while reading/creating raters.json " + file.getAbsolutePath() + " using default!");
            LOGGER.error(e.getMessage(), e);
        }

        return ratersJSON;

    }

    ////////////////////////////////
    public void readAPIkeysJSON() {

        File file = new File(getRpcPort() == 9068? "secret-keys-demo.json" : "secret-keys.json");
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
