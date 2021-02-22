package org.erachain.dextrader.Raters;


import org.erachain.dextrader.settings.Settings;
import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// see https://pro.coinmarketcap.com/account/
/// result   "last":9577.769,"buy":9577.769,"sell":9509.466
public class RaterCoinMarketCapCom extends Rater {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaterCoinMarketCapCom.class);

    static String NAME = "coinmarketcap.com";

    public RaterCoinMarketCapCom(TradersManager tradersManager, int sleepSec) {
        super(tradersManager, NAME, null, "https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest",
                sleepSec);

        headers = new HashMap<>();
        headers.put("Accept","application/json");
        headers.put("X-CMC_PRO_API_KEY", Settings.getInstance().apiKeysJSON.get(NAME).toString());

    }


    private BigDecimal calcPrice(BigDecimal rateBuy, BigDecimal rateSell, BigDecimal rateLast) {
        try {
            return (rateBuy.add(rateSell).divide(new BigDecimal(2), 10, BigDecimal.ROUND_HALF_UP))
                .multiply(this.shiftRate).setScale(10, BigDecimal.ROUND_HALF_UP);
        } catch (NullPointerException | ClassCastException e) {
            return null;
        }

    }

    public void clearRates() {
        if (cnt.DEVELOP_USE) {
            rates.remove(makeKey(1106L, 1105L, this.courseName));
        } else {
            rates.remove(makeKey(12L, 95L, this.courseName));
        }
    }

    protected void parse(String result) {
        JSONObject json = null;
        try {
            //READ JSON
            json = (JSONObject) JSONValue.parse(result);
        } catch (NullPointerException | ClassCastException e) {
            //JSON EXCEPTION
            LOGGER.error(e.getMessage(), e);
            throw e;
        }

        if (json == null)
            return;

        //logger.info("WEX : " + result);

        JSONObject pair;
        BigDecimal price;
        BigDecimal rateLast = null;
        BigDecimal rateBuy = null;
        BigDecimal rateSell = null;

        if (json.containsKey("btc_rur")) {
            rateBuy = null;
            pair = (JSONObject) json.get("btc_rur");
            try {
                rateLast = new BigDecimal(pair.get("last").toString());
                rateBuy = new BigDecimal(pair.get("buy").toString());
                rateSell = new BigDecimal(pair.get("sell").toString());
            } catch (Exception e){
                LOGGER.error(e.getMessage(), e);
            }

            if (rateBuy != null) {
                price = calcPrice(rateBuy, rateSell, rateLast);
                if (cnt.DEVELOP_USE) {
                    setRate(1105L, 1108L, this.courseName, price);
                } else {
                    setRate(12L, 92L, this.courseName, price);
                }
            }

        }

        if (json.containsKey("btc_usd")) {
            rateBuy = null;
            pair = (JSONObject) json.get("btc_usd");
            try {
                rateLast = new BigDecimal(pair.get("last").toString());
                rateBuy = new BigDecimal(pair.get("buy").toString());
                rateSell = new BigDecimal(pair.get("sell").toString());
            } catch (Exception e){
                LOGGER.error(e.getMessage(), e);
            }

            if (rateBuy != null) {
                price = calcPrice(rateBuy, rateSell, rateLast);
                if (cnt.DEVELOP_USE) {
                    setRate(1105L, 1107L, this.courseName, price);
                } else {
                    setRate(12L, 95L, this.courseName, price);
                }
            }

        }

        if (json.containsKey("usd_rur")) {
            rateBuy = null;
            pair = (JSONObject) json.get("usd_rur");
            try {
                rateLast = new BigDecimal(pair.get("last").toString());
                rateBuy = new BigDecimal(pair.get("buy").toString());
                rateSell = new BigDecimal(pair.get("sell").toString());
            } catch (Exception e){
                    LOGGER.error(e.getMessage(), e);
            }

            if (rateBuy != null) {
                price = calcPrice(rateBuy, rateSell, rateLast);
                setRate(1077L, 1078L, this.courseName, price);
                LOGGER.info("WEX rate: USD - RUR " + price);
            }

        }

    }
}
