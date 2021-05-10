package org.erachain.dextrader.Raters;

import org.erachain.dextrader.traders.TradersManager;
import org.json.simple.JSONObject;

import java.math.BigDecimal;

public class RaterBitforexCom extends RaterOnePair {

    public RaterBitforexCom(TradersManager tradersManager, int sleepSec) {
        super(tradersManager, "bitforex", "https://api.bitforex.com/api/v1/market/ticker?symbol=", sleepSec, "coin-usdt-gold", 21L, 95L
        );
    }

    @Override
    BigDecimal getValue(JSONObject response) {
        // {"data":{"buy":51.70018,"date":1587563047849,"high":54.62,"last":52.08,"low":48.34,"sell":53.2,"vol":113818.2957},"success":true,"time":1587563047849}
        if (response.containsKey("success")
                && (Boolean) response.get("success")) {
            JSONObject data = (JSONObject) response.get("data");

            BigDecimal buy;
            BigDecimal sell;

            buy = new BigDecimal(data.get("buy").toString()).setScale(5, BigDecimal.ROUND_HALF_UP);
            sell = new BigDecimal(data.get("sell").toString()).setScale(5, BigDecimal.ROUND_HALF_UP);

            return buy.add(sell)
                    // преобразуем в тройскую унцию
                    .multiply(new BigDecimal("31.104"))
                    // среднее арифметическое
                    .multiply(new BigDecimal("0.5"));
        }

        return null;
    }
}
