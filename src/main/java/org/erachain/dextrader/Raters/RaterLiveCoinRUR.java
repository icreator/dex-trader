package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.math.BigDecimal;


public class RaterLiveCoinRUR extends Rater {

    public RaterLiveCoinRUR(TradersManager tradersManager, int sleepSec) {
        super(tradersManager, "livecoin", sleepSec, null);

        this.apiURL = "https://api.livecoin.net/exchange/ticker?currencyPair=BTC/RUR";

    }

    public void clearRates() {
        if (cnt.DEVELOP_USE) {
            rates.remove(makeKey(1105L, 1108L, this.courseName));
        } else {
            rates.remove(makeKey(12L, 92L, this.courseName));
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
                && "BTC/RUR".equals((String)json.get("symbol"))) {
            price = new BigDecimal(json.get("vwap").toString()).setScale(10, BigDecimal.ROUND_HALF_UP);
            //price = price.multiply(this.shiftRate).setScale(10, BigDecimal.ROUND_HALF_UP);
            if (cnt.DEVELOP_USE) {
                setRate(1105L, 1108L, this.courseName, price);
            } else {
                setRate(12L, 92L, this.courseName, price);
            }
        }

    }
}
