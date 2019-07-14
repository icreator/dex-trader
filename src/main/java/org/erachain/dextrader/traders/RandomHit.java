package org.erachain.dextrader.traders;

import org.erachain.dextrader.Raters.Rater;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * ставим обреда случайно вс такан "ПО РЫНКУ"
 * и иногда не сыгравшие ордера снимаем
 */
public class RandomHit extends Trader {

    private int steep;
    Random random = new Random();
    private List<BigDecimal> keys;
    private long sleepOrig;

    public RandomHit(TradersManager tradersManager, String accountStr, int sleepSec, long haveKey, long wantKey,
                     String sourceExchange, TreeMap<BigDecimal, BigDecimal> scheme, BigDecimal limitUP, BigDecimal limitDown, boolean cleanAllOnStart) {
        super(tradersManager, accountStr, sleepSec, sourceExchange, scheme, haveKey, wantKey, cleanAllOnStart, limitUP, limitDown);
        keys = new ArrayList<BigDecimal>(this.scheme.keySet());
        sleepOrig = this.sleepTimestep;

    }

    public RandomHit(TradersManager tradersManager, String accountStr, JSONObject json) {
        super(tradersManager, accountStr, json);
        keys = new ArrayList<BigDecimal>(this.scheme.keySet());
        sleepOrig = this.sleepTimestep;
    }

    protected boolean createOrder(BigDecimal schemeAmount) {

        BigDecimal shiftPercentageOrig = this.scheme.get(schemeAmount);
        BigDecimal shiftPercentageHalf = shiftPercentageOrig.setScale(5, RoundingMode.HALF_DOWN)
                .divide(BigDecimal.valueOf(2), RoundingMode.HALF_DOWN);
        int randomShift = random.nextInt(1000);
        BigDecimal ggg = new BigDecimal(randomShift).multiply(new BigDecimal("0.001"));
        BigDecimal shiftPercentage = shiftPercentageOrig.subtract(shiftPercentageHalf)
                .add(shiftPercentageOrig.multiply(ggg));

        long haveKey;
        long wantKey;

        String haveName;
        String wantName;

        BigDecimal amountHave;
        BigDecimal amountWant;

        if (schemeAmount.signum() > 0) {
            haveKey = this.haveAssetKey;
            haveName = this.haveAssetName;
            wantKey = this.wantAssetKey;
            wantName = this.wantAssetName;


            BigDecimal shift = BigDecimal.ONE.add(shiftPercentage.movePointLeft(2));

            amountHave = schemeAmount.stripTrailingZeros();
            amountWant = amountHave.multiply(this.rate).multiply(shift).stripTrailingZeros();

            // NEED SCALE for VALIDATE
            if (amountWant.scale() > this.wantAssetScale) {
                amountWant = amountWant.setScale(wantAssetScale, BigDecimal.ROUND_HALF_UP);
            }

        } else {
            haveKey = this.wantAssetKey;
            haveName = this.wantAssetName;
            wantKey = this.haveAssetKey;
            wantName = this.haveAssetName;

            BigDecimal shift = BigDecimal.ONE.subtract(shiftPercentage.movePointLeft(2));

            amountWant = schemeAmount.negate().stripTrailingZeros();
            amountHave = amountWant.multiply(this.rate).multiply(shift).stripTrailingZeros();

            // NEED SCALE for VALIDATE
            if (amountHave.scale() > this.wantAssetScale) {
                amountHave = amountHave.setScale(wantAssetScale, BigDecimal.ROUND_HALF_UP);
            }
        }

        return super.createOrder(schemeAmount, haveKey, haveName, amountHave, wantKey, wantName, amountWant);

    }

    @Override
    public boolean updateCap() {

        BigDecimal schemeAmount = keys.get(random.nextInt((keys.size())));
        return createOrder(schemeAmount);

    }

    protected boolean process() {

        BigDecimal newRate = Rater.getRate(this.haveAssetKey, this.wantAssetKey, sourceExchange);

        if (newRate == null) {            // если курса нет то отменим все ордера и ждем
            LOGGER.info("Rate " + this.haveAssetKey + "/" + this.wantAssetKey +  " not found - clear all orders and awaiting...");
            cleanSchemeOrders();
            return false;
        }

        rate = newRate;
        steep++;

        if (steep > 3) {
            steep = 0;
            cleanSchemeOrders();
            this.sleepTimestep = sleepOrig >> 2;

            return false;
        }

        boolean result = updateCap();
        if (result) {
            this.sleepTimestep = (sleepOrig >> 1) + random.nextInt((int)sleepOrig);
        } else {
            this.sleepTimestep = 30000 + (sleepOrig >> 4);
        }

        return result;
    }

}
