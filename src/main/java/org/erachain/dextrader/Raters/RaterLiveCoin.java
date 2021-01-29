package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.math.BigDecimal;


public class RaterLiveCoin extends Rater {

    public RaterLiveCoin(TradersManager tradersManager, int sleepSec) {
        super(tradersManager, "livecoin", sleepSec, null);

        this.apiURL = "https://api.livecoin.net/exchange/ticker?currencyPair=ETH/BTC";

    }

    public void clearRates() {
        if (cnt.DEVELOP_USE) {
            rates.remove(makeKey(1106L, 1105L, this.courseName));
        } else {
            rates.remove(makeKey(14L, 12L, this.courseName));
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

        JSONObject pair;
        BigDecimal price;

        if (json.containsKey("symbol")
                && "ETH/BTC".equals((String)json.get("symbol"))) {
            price = new BigDecimal(json.get("vwap").toString()).setScale(10, BigDecimal.ROUND_HALF_UP);
            if (cnt.DEVELOP_USE) {
                setRate(1106L, 1105L, this.courseName, price);
            } else {
                setRate(14L, 12L, this.courseName, price);
            }
        }

    }
}
