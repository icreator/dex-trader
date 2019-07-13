package org.erachain.dextrader.traders;

import org.erachain.dextrader.Raters.Rater;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * ставим обреда случайно вс такан "ПО РЫНКУ"
 * в схеме всего 2 значения - положительное и отрицательное
 * и иногда не сыгравшие ордера снимаем
 */
public class RandomHitSelf extends Trader {

    private int steep;
    Random random = new Random();
    private List<BigDecimal> keys;
    private long sleepOrig;

    public RandomHitSelf(TradersManager tradersManager, String accountStr, int sleepSec, long haveKey, long wantKey,
                         String sourceExchange, HashMap<BigDecimal, BigDecimal> scheme, BigDecimal limitUP, BigDecimal limitDown, boolean cleanAllOnStart) {
        super(tradersManager, accountStr, sleepSec, sourceExchange, scheme, haveKey, wantKey, cleanAllOnStart, limitUP, limitDown);
        keys = new ArrayList<BigDecimal>(this.scheme.keySet());
        sleepOrig = this.sleepTimestep;

    }

    public RandomHitSelf(TradersManager tradersManager, String accountStr, JSONObject json) {
        super(tradersManager, accountStr, json);
        keys = new ArrayList<BigDecimal>(this.scheme.keySet());
        sleepOrig = this.sleepTimestep;
    }

    protected boolean createOrder(BigDecimal schemeAmount, JSONObject order) {

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

            amountHave = new BigDecimal(order.get("amount").toString());
            if (schemeAmount.compareTo(amountHave) < 0) {
                amountHave = schemeAmount.stripTrailingZeros();
            }
            amountWant = amountHave.multiply(new BigDecimal(order.get("price").toString())).stripTrailingZeros();


            // NEED SCALE for VALIDATE
            if (amountWant.scale() > this.wantAssetScale) {
                amountWant = amountWant.setScale(wantAssetScale, BigDecimal.ROUND_HALF_UP);
            }

        } else {
            haveKey = this.wantAssetKey;
            haveName = this.wantAssetName;
            wantKey = this.haveAssetKey;
            wantName = this.haveAssetName;

            amountWant = new BigDecimal(order.get("amount").toString());
            if (schemeAmount.negate().compareTo(amountWant) < 0) {
                amountWant = schemeAmount.negate().stripTrailingZeros();
            }

            amountHave = amountWant.multiply(new BigDecimal(order.get("price").toString())).stripTrailingZeros();

            // NEED SCALE for VALIDATE
            if (amountHave.scale() > this.wantAssetScale) {
                amountHave = amountHave.setScale(wantAssetScale, BigDecimal.ROUND_HALF_UP);
            }
        }

        return super.createOrder(schemeAmount, haveKey, haveName, amountHave, wantKey, wantName, amountWant);

    }

    @Override
    public boolean updateCap() {

        JSONObject orders = getCapOrders(haveAssetKey, wantAssetKey, 1);
        if (orders.isEmpty())
            return false;

        BigDecimal schemeAmount = keys.get(random.nextInt(2));
        JSONArray ordersDo;
        if (schemeAmount.signum() > 0) {
            // продажа - берем встречные ордера
            ordersDo = (JSONArray) orders.get("want");
        } else {
            // покупка
            ordersDo = (JSONArray) orders.get("have");
            return false;
        }

        if (ordersDo.isEmpty())
            return false;

        return createOrder(schemeAmount, (JSONObject)ordersDo.get(0));

    }

    protected boolean process() {

        steep++;

        if (steep > 5) {
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
