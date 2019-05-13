package org.erachain.eratrader.traders;
// 30/03 ++

import org.erachain.eratrader.controller.Controller;
import org.erachain.eratrader.settings.Settings;
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

    Controller cnt;

    public TradersManager(Controller cnt) {
        this.cnt = cnt;
        this.knownRaters = new ArrayList<Rater>();
        this.knownTraders = new ArrayList<Trader>();

        this.start();
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

        if (false) {

            String sourceExchange = "polonex";
            String address = "7NhZBb8Ce1H2S2MkPerrMnKLZNf9ryNYtP";

            BigDecimal limit1 = new BigDecimal("0.01");
            BigDecimal limit2 = new BigDecimal("0.02");
            if (true) {
                //START TRADERs THREADs
                HashMap<BigDecimal, BigDecimal> schemeUSD_RUB = new HashMap<>();
                schemeUSD_RUB.put(new BigDecimal(1000), new BigDecimal("0.1"));
                schemeUSD_RUB.put(new BigDecimal(100), new BigDecimal("0.03"));
                schemeUSD_RUB.put(new BigDecimal(10), new BigDecimal("0.01"));
                schemeUSD_RUB.put(new BigDecimal(-10), new BigDecimal("0.01"));
                schemeUSD_RUB.put(new BigDecimal(-100), new BigDecimal("0.03"));
                schemeUSD_RUB.put(new BigDecimal(-1000), new BigDecimal("0.1"));
                Trader trader1 = new StoneGuardAbs(this, address,
                        Controller.GENERATING_MIN_BLOCK_TIME_MS,
                        1077, 1078, sourceExchange, schemeUSD_RUB, limit1, limit1, true);
                this.knownTraders.add(trader1);

                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }

            }

            if (true) {
                BigDecimal limit = new BigDecimal("0.3");
                //START TRADERs THREADs
                HashMap<BigDecimal, BigDecimal> schemeUSD_RUB = new HashMap<>();
                schemeUSD_RUB.put(new BigDecimal(30000), new BigDecimal("1.0"));
                schemeUSD_RUB.put(new BigDecimal(10000), new BigDecimal("0.7"));
                schemeUSD_RUB.put(new BigDecimal(100), new BigDecimal("0.3"));
                schemeUSD_RUB.put(new BigDecimal(-100), new BigDecimal("0.3"));
                schemeUSD_RUB.put(new BigDecimal(-10000), new BigDecimal("0.7"));
                schemeUSD_RUB.put(new BigDecimal(-30000), new BigDecimal("1.0"));
                Trader trader1 = new StoneGuardAbs(this, address,
                        Controller.GENERATING_MIN_BLOCK_TIME_MS << 1,
                        1077, 1078, sourceExchange, schemeUSD_RUB, limit, limit, true);
                this.knownTraders.add(trader1);

                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }

            }

            if (true) {
                HashMap<BigDecimal, BigDecimal> schemeBTC_USD = new HashMap<>();
                schemeBTC_USD.put(new BigDecimal(1), new BigDecimal("0.5"));
                schemeBTC_USD.put(new BigDecimal("0.1"), new BigDecimal("0.2"));
                schemeBTC_USD.put(new BigDecimal("0.01"), new BigDecimal("0.05")); // !!!! FOR GOOD SCALE USE STRING - not DOUBLE
                schemeBTC_USD.put(new BigDecimal("-0.01"), new BigDecimal("0.05"));
                schemeBTC_USD.put(new BigDecimal("-0.1"), new BigDecimal("0.2"));
                schemeBTC_USD.put(new BigDecimal(-1), new BigDecimal("0.5"));
                Trader trader2 = new StoneGuard(this, address,
                        Controller.GENERATING_MIN_BLOCK_TIME_MS,
                        1079, 1077, sourceExchange, schemeBTC_USD, limit1, limit1, true);
                this.knownTraders.add(trader2);

                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }

            if (true) {
                BigDecimal limit = new BigDecimal("0.5");
                HashMap<BigDecimal, BigDecimal> schemeBTC_USD = new HashMap<>();
                schemeBTC_USD.put(new BigDecimal(7), new BigDecimal("1"));
                schemeBTC_USD.put(new BigDecimal(3), new BigDecimal("0.8"));
                schemeBTC_USD.put(new BigDecimal(-3), new BigDecimal("0.8"));
                schemeBTC_USD.put(new BigDecimal(-7), new BigDecimal("1.0"));
                Trader trader2 = new StoneGuard(this, address,
                        Controller.GENERATING_MIN_BLOCK_TIME_MS << 1,
                        1079, 1077, sourceExchange, schemeBTC_USD, limit, limit, true);
                this.knownTraders.add(trader2);

                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }

            if (true) {
                //START TRADER COMPU <> ERA
                HashMap<BigDecimal, BigDecimal> schemeCOMPU_ERA = new HashMap<>();
                schemeCOMPU_ERA.put(new BigDecimal("0.1"), new BigDecimal("2"));
                schemeCOMPU_ERA.put(new BigDecimal("0.01"), new BigDecimal("1"));
                schemeCOMPU_ERA.put(new BigDecimal("-0.01"), new BigDecimal("1"));
                schemeCOMPU_ERA.put(new BigDecimal("-0.1"), new BigDecimal("2"));
                Trader trader = new StoneGuard(this, address,
                        Controller.GENERATING_MIN_BLOCK_TIME_MS,
                        2, 1, sourceExchange, schemeCOMPU_ERA, limit2, limit2, true);
                this.knownTraders.add(trader);

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }

        }

        if ( this.knownTraders.isEmpty()) {
            LOGGER.error("not found traders Accounts");
            cnt.stopAll(-13);
        }
    }

    public void setRun(boolean status) {

        for (Rater rater: this.knownRaters) {
            rater.setRun(status);
        }
    }

    public void stop() {

        for (Rater rater: this.knownRaters) {
            rater.setRun(false);
        }
    }
}
