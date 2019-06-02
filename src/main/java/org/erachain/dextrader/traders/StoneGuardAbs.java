package org.erachain.dextrader.traders;
// 30/03 ++

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;


/**
 * shift as ABSOLUTE in scheme
 */
public class StoneGuardAbs extends Trader {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoneGuardAbs.class);

    public StoneGuardAbs(TradersManager tradersManager, String accountStr, int sleepSec, long haveKey, long wantKey,
                         String sourceExchange, HashMap<BigDecimal, BigDecimal> scheme, BigDecimal limitUP, BigDecimal limitDown, boolean cleanAllOnStart) {
        super(tradersManager, accountStr, sleepSec, sourceExchange, scheme, haveKey, wantKey, cleanAllOnStart,
                limitUP, limitDown);
    }

    protected boolean createOrder(BigDecimal schemeAmount) {

        String result;
        BigDecimal shiftAbsolute = this.scheme.get(schemeAmount);

        // защита от слишком маленьких значений
        int percentage = new BigDecimal("100").multiply(shiftAbsolute).divide(this.rate, 0, BigDecimal.ROUND_HALF_UP).abs().intValue();
        if (percentage > 10) {
            LOGGER.error(">>>>> SO BIG SHIFT for " + this.haveAssetName
                    + "/" + this.wantAssetName + " to " + this.rate.toString() + " - " + shiftAbsolute.toPlainString());
            return true;
        }

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

            amountHave = schemeAmount.stripTrailingZeros();
            amountWant = amountHave.multiply(this.rate.add(shiftAbsolute)).stripTrailingZeros();

            // NEED SCALE for VALIDATE
            if (amountWant.scale() > this.wantAssetScale) {
                amountWant = amountWant.setScale(wantAssetScale, BigDecimal.ROUND_HALF_UP);
            }

        } else {
            haveKey = this.wantAssetKey;
            haveName = this.wantAssetName;
            wantKey = this.haveAssetKey;
            wantName = this.haveAssetName;

            amountWant = schemeAmount.negate().stripTrailingZeros();
            amountHave = amountWant.multiply(this.rate.subtract(shiftAbsolute)).stripTrailingZeros();

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

        if (newRate == null) {
            // если курса нет то отменим все ордера и ждем
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

            // IN ABSOLUTE
            BigDecimal diffAbs = newRate.subtract(this.rate);
            if (//BlockChain.DEVELOP_USE ||
                    diffAbs.compareTo(this.limitUP) > 0
                    || diffAbs.abs().compareTo(this.limitDown) > 0) {

                this.rate = newRate;
                shiftAll();
            } else {
                return updateCap();
            }
        }

        return true;
    }

}
