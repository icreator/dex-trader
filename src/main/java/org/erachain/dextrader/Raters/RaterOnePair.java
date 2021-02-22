package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.math.BigDecimal;


/**
 * https://binance-docs.github.io/apidocs/spot/en/#kline-candlestick-data
 */
public abstract class RaterOnePair extends Rater {

    final String pair;
    final long baseKey;
    final long quoteKey;

    public RaterOnePair(TradersManager tradersManager, String name, String pair, long baseKey, long quoteKey, String apiURL, int sleepSec) {
        super(tradersManager, name + "." + pair, name, apiURL + pair, sleepSec);

        this.baseKey = baseKey;
        this.quoteKey = quoteKey;

        this.pair = pair;

    }

    public void clearRates() {
        rates.remove(makeKey(baseKey, quoteKey, this.courseName));
    }

    abstract BigDecimal getValue(JSONObject response);

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

        BigDecimal price = getValue(json);
        if (price != null) {
            setRate(baseKey, quoteKey, this.courseName, price);
        }
    }

}
