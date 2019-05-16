package org.erachain.dextrader.traders;
// 30/03 ++

import org.erachain.dextrader.controller.Controller;
import org.erachain.dextrader.settings.Settings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;


public class TradersManager {

    protected static final String WALLET_PASSWORD = "123456789";

    private static final Logger LOGGER = LoggerFactory.getLogger(TradersManager.class);

    private List<Rater> knownRaters;
    private List<Trader> knownTraders;

    // ID -> NAME, SCALE
    protected HashMap<Long, Pair<String, Integer>> assets = new HashMap<>();

    Controller cnt;

    public TradersManager(Controller cnt) {
        this.cnt = cnt;
        this.knownRaters = new ArrayList<Rater>();
        this.knownTraders = new ArrayList<Trader>();

        this.start();
    }

    public class Pair<T, U> {

        public final T a;
        public final U b;

        public Pair(T a, U b) {
            this.a = a;
            this.b = b;
        }

    }

    private void start() {

        String result = cnt.apiClient.executeCommand("GET addresses/" + "?password=" + TradersManager.WALLET_PASSWORD);

        JSONArray walletAddresses;
        try {
            //READ JSON
            walletAddresses = (JSONArray) JSONValue.parse(result);
            LOGGER.info(walletAddresses.toJSONString());

        } catch (NullPointerException | ClassCastException e) {
            //JSON EXCEPTION
            LOGGER.error(e.getMessage());
            cnt.stopAll(-11);
            return;
        } finally {
            // CLOSE SECRET WALLET
            cnt.apiClient.executeCommand("GET wallet/lock");
        }

        if (false) {
            //START RATERs THREADs
            RaterWEX raterForex = new RaterWEX(this, 300);
            this.knownRaters.add(raterForex);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }

        if (true) {
            RaterLiveCoin raterLiveCoin = new RaterLiveCoin(this, 600);
            this.knownRaters.add(raterLiveCoin);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }

        }
        if (true) {
            RaterLiveCoinRUR raterLiveCoinRUR = new RaterLiveCoinRUR(this, 600);
            this.knownRaters.add(raterLiveCoinRUR);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }

        }

        if (true) {
            RaterPolonex raterPolonex = new RaterPolonex(this, 600);
            this.knownRaters.add(raterPolonex);
        }


        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }

        for (Object obj: Settings.getInstance().tradersJSON) {
            JSONObject json = (JSONObject) obj;

            String traderAddress = json.get("traderAddress").toString();
            if (!walletAddresses.contains(traderAddress)) {
                LOGGER.error("not found traders Account - " + traderAddress);
                continue;
            }

            boolean absolute = false;
            try {
                absolute = (boolean) json.get("absoluteAmount");
            } catch (Exception e) {

            }
            boolean cleanAllOnStart = false;
            try {
                cleanAllOnStart = (boolean) json.get("cleanAllOnStart");
            } catch (Exception e) {

            }

            HashMap<BigDecimal, BigDecimal> scheme = new HashMap<>();
            JSONObject schemeJSON = (JSONObject) json.get("scheme");
            for (Object key: schemeJSON.keySet()) {
                scheme.put(new BigDecimal(key.toString()),
                        new BigDecimal(schemeJSON.get(key).toString()));
            }

            Trader trader;
            if (absolute) {
                trader = new StoneGuardAbs(this, traderAddress,
                        (int)(long)json.get("sleepTime"),
                        (long)json.get("haveAssetKey"),
                        (long)json.get("wantAssetKey"),
                        json.get("sourceExchange").toString(),
                        scheme,
                        new BigDecimal(json.get("limitUP").toString()),
                        new BigDecimal(json.get("limitDown").toString()),
                        cleanAllOnStart);
            } else {
                trader = new StoneGuard(this, json.get("traderAddress").toString(),
                        (int)(long)json.get("sleepTime"),
                        (long)json.get("haveAssetKey"),
                        (long)json.get("wantAssetKey"),
                        json.get("sourceExchange").toString(),
                        scheme,
                        new BigDecimal(json.get("limitUP").toString()),
                        new BigDecimal(json.get("limitDown").toString()),
                        cleanAllOnStart);
            }
            this.knownTraders.add(trader);

        }

        if ( this.knownTraders.isEmpty()) {
            LOGGER.error("not found traders Accounts");
            cnt.stopAll(-13);
        }
    }

    public Pair<String, Integer> getAsset(Long key) {

        if (assets.containsKey(key)) {
            return assets.get(key);
        }

        // IF that TRANSACTION exist in CHAIN or queue
        String result = cnt.apiClient.executeCommand("GET assets/" + key);
        try {
            //READ JSON
            JSONObject json = (JSONObject) JSONValue.parse(result);
            Pair pair = new Pair<String, Integer>(json.get("name").toString(), (int)(long)json.get("scale"));
            assets.put(key, pair);
            return pair;

        } catch (NullPointerException | ClassCastException e) {
            //JSON EXCEPTION
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    public void stop() {

        for (Rater rater: this.knownRaters) {
            rater.close();
            try {
                rater.join();
            } catch (Exception e) {

            }
        }
        for (Trader trader: this.knownTraders) {
            trader.close();
            try {
                trader.join();
            } catch (Exception e) {

            }
        }
    }
}
