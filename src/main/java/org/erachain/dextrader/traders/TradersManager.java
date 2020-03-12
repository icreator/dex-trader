package org.erachain.dextrader.traders;
// 30/03 ++

import org.erachain.dextrader.Raters.*;
import org.erachain.dextrader.controller.Controller;
import org.erachain.dextrader.settings.Settings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            LOGGER.error(e.getMessage(), e);
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
        if (false) {
            //START RATERs THREADs
            RaterCoinMarketCapCom raterMarcetCap = new RaterCoinMarketCapCom(this, 300);
            this.knownRaters.add(raterMarcetCap);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }


        if (true) {
            RaterLiveCoin raterLiveCoin = new RaterLiveCoin(this, 300);
            this.knownRaters.add(raterLiveCoin);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }

        }
        if (true) {
            RaterLiveCoinRUR raterLiveCoinRUR = new RaterLiveCoinRUR(this, 300);
            this.knownRaters.add(raterLiveCoinRUR);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }

        }

        if (true) {
            RaterPolonex raterPolonex = new RaterPolonex(this, 300);
            this.knownRaters.add(raterPolonex);
        }

        if (true) {
            RaterCross raterCross_ETH_RUB = new RaterCross(this, 300, "ETH_RUB",
                    new String[]{"14.12 polonex", "12.92 livecoin"});
            this.knownRaters.add(raterCross_ETH_RUB);
        }

        //if (true) return;

        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }

        for (Object obj : Settings.getInstance().tradersJSON) {

            JSONObject item = (JSONObject) obj;
            String traderAddress = item.get("traderAddress").toString();
            if (!walletAddresses.contains(traderAddress)) {
                LOGGER.error("not found traders Account - " + traderAddress);
                continue;
            }

            String type = (String) item.get("type");
            Trader trader = null;

            if (type.equals("Guard")) {
                trader = new StoneGuard(this, traderAddress, item);
            } else if (type.equals("GuardAbs")) {
                trader = new StoneGuardAbs(this, traderAddress, item);
            } else if (type.equals("RandomHit")) {
                trader = new RandomHit(this, traderAddress, item);
            } else if (type.equals("RandomHitSelf")) {
                trader = new RandomHitSelf(this, traderAddress, item);
            }

            if (trader != null) {
                this.knownTraders.add(trader);
            }

        }

        if ( this.knownTraders.isEmpty()) {
            LOGGER.error("Not found Traders Accounts or Traders in Settings");
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
