package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class RaterPolonex extends Rater {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaterPolonex.class);

    // https://poloniex.com/support/api/v1/
    // https://poloniex.com/public?command=returnTicker&
    // https://poloniex.com/public?command=returnTicker&pair=BTC_ETH - return ALL
    public RaterPolonex(TradersManager tradersManager, int sleepSec) {
        super(tradersManager,"polonex", null,  "https://poloniex.com/public?command=returnTicker",
                sleepSec);

    }

    public void clearRates() {
        if (cnt.DEVELOP_USE) {
            rates.remove(makeKey(1105L, 1106L, this.courseName));
        } else {
            rates.remove(makeKey(12L, 95L, this.courseName));
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

        if (json.containsKey("USDT_BTC")) {
            pair = (JSONObject) json.get("USDT_BTC");
            price = new BigDecimal(pair.get("last").toString());
            /// BACK price
            //price = price.multiply(this.shiftRate).setScale(10, BigDecimal.ROUND_HALF_UP);
            if (cnt.DEVELOP_USE) {
                setRate(1105L, 1107L, this.courseName, price);
            } else {
                setRate(12L, 95L, this.courseName, price);
            }
        }

        if (json.containsKey("BTC_ETH")) {
            pair = (JSONObject) json.get("BTC_ETH");
            price = new BigDecimal(pair.get("last").toString());
            /// BACK price
            //price = price.multiply(this.shiftRate).setScale(10, BigDecimal.ROUND_HALF_UP);
            if (cnt.DEVELOP_USE) {
                setRate(1106L, 1105L, this.courseName, price);
            } else {
                setRate(14L, 12L, this.courseName, price);
            }
        }

    }
}
