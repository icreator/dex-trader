package org.erachain.dextrader.traders;

import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.TreeMap;

/**
 * ставим ордера случайно в стакан по найденным ордерам ближайшим + случайное количество + случайна цена на по рынку 1 %
 * в схеме всего 2 значения - положительное и отрицательное
 * и иногда не сыгравшие ордера снимаем
 */
public class RandomHitSelfRandPrice extends RandomHitSelfRand {

    public RandomHitSelfRandPrice(TradersManager tradersManager, String accountStr, int sleepSec, long haveKey, long wantKey,
                                  String sourceExchange, TreeMap<BigDecimal, BigDecimal> scheme, BigDecimal limitUP, BigDecimal limitDown, boolean cleanAllOnStart) {
        super(tradersManager, accountStr, sleepSec, haveKey, wantKey,
                sourceExchange, scheme, limitUP, limitDown, cleanAllOnStart);

    }

    public RandomHitSelfRandPrice(TradersManager tradersManager, String accountStr, JSONObject json) {
        super(tradersManager, accountStr, json);
    }

    @Override
    BigDecimal getRandAmountForPrice(BigDecimal schemeAmount, boolean forSell) {
        double diff = 1.0 + 0.001 * random.nextInt(10);
        if (forSell)
            diff = 1.0d / diff;

        return schemeAmount.multiply(new BigDecimal(
                random.nextBoolean()? diff : 1.0 / diff).setScale(5, RoundingMode.UP));
    }

}
