package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RaterPolonex extends RaterManyPairs {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaterPolonex.class);

    // https://poloniex.com/support/api/v1/
    // https://poloniex.com/public?command=returnTicker&
    // https://poloniex.com/public?command=returnTicker&pair=BTC_ETH - return ALL
    public RaterPolonex(TradersManager tradersManager, JSONObject pairs, int sleepSec) {
        super(tradersManager,"polonex",
                "https://poloniex.com/public?command=returnTicker",
                sleepSec, pairs);
    }

    @Override
    BigDecimal getValue(JSONObject response, String pairName, Long baseKey, Long quoteKey) {
        if (response.containsKey(pairName)) {
            JSONObject pair = (JSONObject) response.get(pairName);
            try {
                return BigDecimal.ONE.divide(new BigDecimal(pair.get("last").toString()), 12, RoundingMode.HALF_DOWN);
            } catch (Exception e) {
            }
        }
        return null;
    }

}
