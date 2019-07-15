package org.erachain.dextrader.traders;

import org.erachain.dextrader.Raters.Rater;
import org.erachain.dextrader.controller.Controller;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * shift as PERCENTS in scheme
 */
public class StoneGuard extends Trader {

    public StoneGuard(TradersManager tradersManager, String accountStr, int sleepSec, long haveKey, long wantKey,
                      String sourceExchange, TreeMap<BigDecimal, BigDecimal> scheme, BigDecimal limitUP, BigDecimal limitDown, boolean cleanAllOnStart) {
        super(tradersManager, accountStr, sleepSec, sourceExchange, scheme, haveKey, wantKey, cleanAllOnStart, limitUP, limitDown);

    }

    public StoneGuard(TradersManager tradersManager, String accountStr, JSONObject json) {
        super(tradersManager, accountStr, json);
    }

    protected boolean createOrder(BigDecimal schemeAmount) {

        String result;
        BigDecimal shiftPercentage = this.scheme.get(schemeAmount);

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

    private void shiftAll() {

        LOGGER.info(">>>>> shift ALL for " + this.haveAssetName
                + "/" + this.wantAssetName + " to " + this.rate.toString());

        // REMOVE ALL ORDERS
        if (cleanSchemeOrders()) {

            try {
                //Thread.sleep(BlockChain.GENERATING_MIN_BLOCK_TIME_MS);
                sleep(4000);
            } catch (Exception e) {
                //FAILED TO SLEEP
            }
        }

        //BigDecimal persent;
        for (BigDecimal amount : this.scheme.keySet()) {
            //persent = this.scheme.get(amount);
            createOrder(amount);
            try {
                sleep(100);
            } catch (Exception e) {
                //FAILED TO SLEEP
            }

        }
    }

    protected boolean process() {

        String callerResult = null;

        BigDecimal newRate = Rater.getRate(this.haveAssetKey, this.wantAssetKey, sourceExchange);

        if (newRate == null) {            // если курса нет то отменим все ордера и ждем
            LOGGER.info("Rate " + this.haveAssetKey + "/" + this.wantAssetKey +  " not found - clear all orders and awaiting...");
            cleanSchemeOrders();
            return false;
        }

        if (this.rate == null) {
            this.rate = newRate;
            shiftAll();

        } else {
            if (//!BlockChain.DEVELOP_USE &&
                    newRate.compareTo(this.rate) == 0)
                return updateCap();

            BigDecimal diffPerc = newRate.divide(this.rate, 8, BigDecimal.ROUND_HALF_UP)
                    .subtract(BigDecimal.ONE).multiply(M100);
            if (Controller.DEVELOP_USE ||
                    diffPerc.compareTo(this.limitUP) > 0
                    || diffPerc.abs().compareTo(this.limitDown) > 0) {

                this.rate = newRate;
                shiftAll();
            } else {
                return updateCap();
            }
        }

        return true;
    }

}
