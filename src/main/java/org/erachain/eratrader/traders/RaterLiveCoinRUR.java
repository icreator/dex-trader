package org.erachain.eratrader.traders;
// 30/03 ++

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.math.BigDecimal;


public class RaterLiveCoinRUR extends Rater {

    public RaterLiveCoinRUR(TradersManager tradersManager, int sleepSec) {
        super(tradersManager, "livecoin", sleepSec);

        this.apiURL = "https://api.livecoin.net/exchange/ticker?currencyPair=BTC/RUR";

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
                setRate(1104L, 1107L, this.courseName, price);
            } else {
                setRate(12L, 95L, this.courseName, price);
            }
        }

    }
}
