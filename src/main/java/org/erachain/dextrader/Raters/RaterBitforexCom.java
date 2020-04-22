package org.erachain.dextrader.Raters;

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.math.BigDecimal;


public class RaterBitforexCom extends Rater {

    public RaterBitforexCom(TradersManager tradersManager, int sleepSec) {
        super(tradersManager, "bitforex", sleepSec);

        this.apiURL = "https://api.bitforex.com/api/v1/market/ticker?symbol=coin-usdt-gold";

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

        BigDecimal buy;
        BigDecimal sell;
        BigDecimal price;

        // {"data":{"buy":51.70018,"date":1587563047849,"high":54.62,"last":52.08,"low":48.34,"sell":53.2,"vol":113818.2957},"success":true,"time":1587563047849}
        if (json.containsKey("success")
                && (Boolean) json.get("success")) {
            buy = new BigDecimal(json.get("buy").toString()).setScale(5, BigDecimal.ROUND_HALF_UP);
            sell = new BigDecimal(json.get("sell").toString()).setScale(5, BigDecimal.ROUND_HALF_UP);

            price = buy.add(sell)
                    // преобразуем в тройскую унцию
                    .multiply(new BigDecimal("31.104"))
                    // среднее арефметическое
                    .multiply(new BigDecimal("0.5"));

            if (cnt.DEVELOP_USE) {
                setRate(1106L, 1105L, this.courseName, price);
            } else {
                setRate(21L, 95L, this.courseName, price);
            }
        }

    }
}
