package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.math.BigDecimal;
import java.util.List;


/**
 * https://poloniex.com/public?command=returnTicker
 */
public abstract class RaterManyPairs extends Rater {

    /**
     * String pair, long baseKey, long quoteKey
     */
    final JSONObject pairs;

    public RaterManyPairs(TradersManager tradersManager, String name, String apiURL, JSONObject pairs, int sleepSec) {
        super(tradersManager, name, name, apiURL, sleepSec);

        this.pairs = pairs;

    }

    public void clearRates() {
        for (Object pair: pairs.values()) {
            rates.remove(makeKey((Long) ((JSONArray)pair).get(0),
                    (Long) ((JSONArray)pair).get(1), this.courseName));
        }
    }

    abstract BigDecimal getValue(JSONObject response, String pairName, Long baseKey, Long quoteKey);

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

        for (Object pairName: pairs.keySet()) {
            JSONArray pair = (JSONArray) pairs.get(pairName);
            BigDecimal price = getValue(json, (String) pairName, (Long)pair.get(0), (Long)pair.get(1));
            setRate((Long)pair.get(0), (Long)pair.get(1), this.courseName, price);
        }
    }

}
