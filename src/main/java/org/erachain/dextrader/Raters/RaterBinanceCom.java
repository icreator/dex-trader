package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.math.BigDecimal;


/**
 * https://binance-docs.github.io/apidocs/spot/en/#kline-candlestick-data
 */
public class RaterBinanceCom extends RaterOnePair {

    // https://api2.binance.com/api/v3/avgPrice?symbol=ETHUSDT
    public RaterBinanceCom(TradersManager tradersManager, int sleepSec, String pair, long baseKey, long quoteKey) {
        super(tradersManager, "binance",pair, baseKey, quoteKey,
                "https://api2.binance.com/api/v3/avgPrice?symbol=" + pair,
                sleepSec, null);
    }

    @Override
    BigDecimal getValue(JSONObject response) {
        if (response.containsKey("price")) {
            return new BigDecimal(response.get("price").toString()).setScale(8, BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }
}
