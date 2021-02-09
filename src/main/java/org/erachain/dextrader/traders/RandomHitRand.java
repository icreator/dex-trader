package org.erachain.dextrader.traders;

import org.erachain.dextrader.Raters.Rater;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.TreeMap;

/**
 * ставим ордера случайно в стакан "ПО РЫНКУ"
 * и иногда не сыгравшие ордера снимаем
 */
public class RandomHitRand extends RandomHit {

    public RandomHitRand(TradersManager tradersManager, String accountStr, int sleepSec, long haveKey, long wantKey,
                         String sourceExchange, TreeMap<BigDecimal, BigDecimal> scheme, BigDecimal limitUP, BigDecimal limitDown, boolean cleanAllOnStart) {
        super(tradersManager, accountStr, sleepSec, haveKey, wantKey,
                sourceExchange, scheme, limitUP, limitDown, cleanAllOnStart);

    }

    public RandomHitRand(TradersManager tradersManager, String accountStr, JSONObject json) {
        super(tradersManager, accountStr, json);
    }

    @Override
    BigDecimal getRandAmount(BigDecimal schemeAmount) {
        int randInt = random.nextInt(50);
        return schemeAmount.multiply(new BigDecimal(1.0 + 0.01 * randInt).setScale(5, RoundingMode.UP));
    }

}
