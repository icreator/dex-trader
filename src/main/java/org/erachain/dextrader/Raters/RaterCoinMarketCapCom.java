package org.erachain.dextrader.Raters;


import org.erachain.dextrader.settings.Settings;
import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * need API-KEY. Set it in secret-keys.json
 * see https://pro.coinmarketcap.com/account/
 */
public class RaterCoinMarketCapCom extends RaterManyPairs {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaterCoinMarketCapCom.class);

    public static String NAME = "coinmarketcap.com";

    public RaterCoinMarketCapCom(TradersManager tradersManager, int sleepSec, JSONObject pairs) {
        super(tradersManager, NAME, "https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest",
                sleepSec, pairs);

        headers = new HashMap<>();
        headers.put("Accept","application/json");
        headers.put("X-CMC_PRO_API_KEY", Settings.getInstance().apiKeysJSON.get(NAME).toString());

    }

    @Override
    BigDecimal getValue(JSONObject response, String pairName, Long baseKey, Long quoteKey) {
        JSONObject item;
        if (response.containsKey("data")) {
            JSONArray array = (JSONArray) response.get("data");
            String[] pairTickets = pairName.split("_");
            for (Object obj: array) {
                item = (JSONObject) obj;
                if (item.get("name").toString().equals(pairTickets[0])) {
                    // HORIZEN
                    return new BigDecimal((double)((JSONObject)((JSONObject)item.get("quote")).get(pairTickets[1])).get("price"))
                            .setScale(16, BigDecimal.ROUND_HALF_UP);
                }
            }
        }
        return null;
    }
}
