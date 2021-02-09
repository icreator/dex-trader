package org.erachain.dextrader.traders;

import org.erachain.dextrader.Raters.Rater;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * ставим ордера случайно в стакан "ПО РЫНКУ"
 * в схеме всего 2 значения - положительное и отрицательное
 * и иногда не сыгравшие ордера снимаем
 */
public class RandomHitSelf extends Trader {

    private int steep;
    Random random = new Random();
    private long sleepOrig;

    public RandomHitSelf(TradersManager tradersManager, String accountStr, int sleepSec, long haveKey, long wantKey,
                         String sourceExchange, TreeMap<BigDecimal, BigDecimal> scheme, BigDecimal limitUP, BigDecimal limitDown, boolean cleanAllOnStart) {
        super(tradersManager, accountStr, sleepSec, sourceExchange, scheme, haveKey, wantKey, cleanAllOnStart, limitUP, limitDown);
        sleepOrig = this.sleepTimestep;

    }

    public RandomHitSelf(TradersManager tradersManager, String accountStr, JSONObject json) {
        super(tradersManager, accountStr, json);
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

            amountHave = new BigDecimal(order.get("pairAmount").toString());
            if (schemeAmount.compareTo(amountHave) < 0) {
                amountHave = schemeAmount.stripTrailingZeros();
            }
            amountWant = amountHave.multiply(new BigDecimal(order.get("pairPrice").toString())).stripTrailingZeros();


            // NEED SCALE for VALIDATE
            if (amountWant.scale() > this.wantAssetScale) {
                amountWant = amountWant.setScale(wantAssetScale, RoundingMode.DOWN);
            }

        } else {
            haveKey = this.wantAssetKey;
            haveName = this.wantAssetName;
            wantKey = this.haveAssetKey;
            wantName = this.haveAssetName;

            amountWant = new BigDecimal(order.get("pairAmount").toString());
            if (schemeAmount.negate().compareTo(amountWant) < 0) {
                amountWant = schemeAmount.negate().stripTrailingZeros();
            }

            amountHave = amountWant.multiply(new BigDecimal(order.get("pairPrice").toString())).stripTrailingZeros();

            // NEED SCALE for VALIDATE
            if (amountHave.scale() > this.wantAssetScale) {
                amountHave = amountHave.setScale(wantAssetScale, RoundingMode.UP);
            }
        }

        return super.createOrder(schemeAmount, haveKey, haveName, amountHave, wantKey, wantName, amountWant);

    }

    @Override
    public boolean updateCap() {

        JSONObject orders = getCapOrders(haveAssetKey, wantAssetKey, 1);
        if (orders.isEmpty())
            return false;

        int schemeIndex = random.nextInt(keys.size());
        BigDecimal schemeAmount = keys.get(schemeIndex);
        JSONArray ordersDo;
        JSONArray ordersDo2;
        if (schemeAmount.signum() > 0) {
            // продажа - берем встречные ордера
            ordersDo = (JSONArray) orders.get("want");
            ordersDo2 = (JSONArray) orders.get("have");
        } else {
            // покупка
            ordersDo = (JSONArray) orders.get("have");
            ordersDo2 = (JSONArray) orders.get("want");
        }

        if (ordersDo.isEmpty())
            return false;

        JSONObject cupOrder = (JSONObject)ordersDo.get(0);
        if (schemeIndex > 0 && schemeIndex < keys.size() - 1) {
            // ставим свой ордер в стакан
            if (ordersDo2.isEmpty())
                return false;

            BigDecimal price1;
            BigDecimal price2;
            BigDecimal priceAdd;
            BigDecimal price;
            if (schemeAmount.signum() > 0) {
                price1 = new BigDecimal(((JSONObject)ordersDo.get(0)).get("pairPrice").toString());
                price2 = new BigDecimal(((JSONObject)ordersDo2.get(0)).get("pairPrice").toString());
            } else {
                price2 = new BigDecimal(((JSONObject)ordersDo.get(0)).get("pairPrice").toString());
                price1 = new BigDecimal(((JSONObject)ordersDo2.get(0)).get("pairPrice").toString());
            }

            priceAdd = price2.subtract(price1).multiply(new BigDecimal(schemeIndex))
                    .divide(new BigDecimal(keys.size() - 1), wantAssetScale, RoundingMode.HALF_DOWN);

            // NEW PRICE
            price = price1.add(priceAdd);
            BigDecimal priceAvg = price2.add(price1).divide(new BigDecimal("2.0"), price1.scale(), RoundingMode.HALF_DOWN);
            BigDecimal priceDiff = priceAvg.subtract(price).abs()
                    .multiply(new BigDecimal("100.0")).divide(priceAvg, 5, RoundingMode.HALF_DOWN);

            if (priceDiff.compareTo(scheme.get(schemeAmount).abs()) > 0) {

                // если запас по сдигу есть то делаем новый свой ордер, иначе выкупаем тот что есть в стакане

                cupOrder.put("pairPrice", price.toPlainString());
                cupOrder.put("pairAmount", schemeAmount.abs().toPlainString());
                // not need cupOrder.put("total", schemeAmount.multiply(price).toPlainString());

            }

        }
        return createOrder(schemeAmount, cupOrder);

    }

    protected boolean process() {

        steep++;

        if (steep > 10) {
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
