package org.erachain.eratrader.traders;
// 30/03 ++

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class RaterPolonex extends Rater {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaterPolonex.class);

    public RaterPolonex(TradersManager tradersManager, int sleepSec) {
        super(tradersManager, "polonex", sleepSec);

        // https://poloniex.com/support/api/v1/
        //https://poloniex.com/public?command=returnTicker&
        //https://poloniex.com/public?command=returnTicker&pair=BTC_ETH - return ALL
        this.apiURL = "https://poloniex.com/public?command=returnTicker";

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

        if (json.containsKey("USDT_BTC")) {
            pair = (JSONObject) json.get("USDT_BTC");
            price = new BigDecimal(pair.get("last").toString());
            price = price.multiply(this.shiftRate).setScale(10, BigDecimal.ROUND_HALF_UP);
            setRate(95L, 12L, this.courseName, price);
        }

    }
}
