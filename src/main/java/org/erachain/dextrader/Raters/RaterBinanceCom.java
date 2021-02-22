package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.math.BigDecimal;


/**
 * https://binance-docs.github.io/apidocs/spot/en/#kline-candlestick-data
 */
public class RaterBinanceCom extends Rater {

    final String pair;
    public RaterBinanceCom(TradersManager tradersManager, int sleepSec, String pair) {
        super(tradersManager, "binance." + pair, sleepSec, null);

        // https://api2.binance.com/api/v3/avgPrice?symbol=ETHUSDT
        this.apiURL = "https://api2.binance.com/api/v3/avgPrice?symbol=BTCUSDT";
        this.pair = pair;

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

        JSONObject pair;
        BigDecimal price;

        if (json.containsKey("price")) {
            price = new BigDecimal(json.get("price").toString()).setScale(8, BigDecimal.ROUND_HALF_UP);
            if (cnt.DEVELOP_USE) {
                setRate(1106L, 1105L, this.courseName, price);
            } else {
                setRate(12L, 95L, this.courseName, price);
            }
        }

    }
}
