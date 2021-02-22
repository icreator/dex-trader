package org.erachain.dextrader.Raters;


import org.erachain.dextrader.settings.Settings;
import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// see https://pro.coinmarketcap.com/account/
public class RaterCoinMarketCapCom extends Rater {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaterCoinMarketCapCom.class);

    public static String NAME = "coinmarketcap.com";

    public RaterCoinMarketCapCom(TradersManager tradersManager, int sleepSec) {
        super(tradersManager, NAME, null, "https://pro-api.coinmarketcap.com/v1/cryptocurrency/listings/latest",
                sleepSec);

        headers = new HashMap<>();
        headers.put("Accept","application/json");
        headers.put("X-CMC_PRO_API_KEY", Settings.getInstance().apiKeysJSON.get(NAME).toString());

    }


    private BigDecimal calcPrice(BigDecimal rateBuy, BigDecimal rateSell, BigDecimal rateLast) {
        try {
            return (rateBuy.add(rateSell).divide(new BigDecimal(2), 10, BigDecimal.ROUND_HALF_UP))
                .multiply(this.shiftRate).setScale(10, BigDecimal.ROUND_HALF_UP);
        } catch (NullPointerException | ClassCastException e) {
            return null;
        }

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

        BigDecimal price;
        JSONObject item;
        if (json.containsKey("data")) {
            JSONArray array = (JSONArray) json.get("data");
            for (Object obj: array) {
                item = (JSONObject) obj;
                if (item.get("name").toString().equals("Horizen")) {
                    // HORIZEN
                    price = new BigDecimal((double)((JSONObject)((JSONObject)item.get("quote")).get("USD")).get("price"))
                            .setScale(16, BigDecimal.ROUND_HALF_UP);
                    if (cnt.DEVELOP_USE) {
                        setRate(1105L, 1108L, this.courseName, price);
                    } else {
                        setRate(12L, 92L, this.courseName, price);
                    }

                }
            }
        }
    }
}
