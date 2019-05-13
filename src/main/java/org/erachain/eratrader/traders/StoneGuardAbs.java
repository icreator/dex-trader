package org.erachain.eratrader.traders;
// 30/03 ++

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;


// shift as ABSOLUTE in scheme
public class StoneGuardAbs extends Trader {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoneGuardAbs.class);

    // in ABSOLUT
    protected BigDecimal limitUP;
    protected BigDecimal limitDown;

    public StoneGuardAbs(TradersManager tradersManager, String accountStr, int sleepSec, long haveKey, long wantKey,
                         HashMap<BigDecimal, BigDecimal> scheme, BigDecimal limitUP, BigDecimal limitDown, boolean cleanAllOnStart) {
        super(tradersManager, accountStr, sleepSec, scheme, haveKey, wantKey, cleanAllOnStart);

        this.limitUP = limitUP;
        this.limitDown = limitDown;

    }

    protected boolean createOrder(BigDecimal schemeAmount) {

        String result;
        BigDecimal shiftAbsolute = this.scheme.get(schemeAmount);

        long haveKey;
        long wantKey;

        BigDecimal amountHave;
        BigDecimal amountWant;

        if (schemeAmount.signum() > 0) {
            haveKey = this.haveAssetKey;
            wantKey = this.wantAssetKey;

            amountHave = schemeAmount.stripTrailingZeros();
            amountWant = amountHave.multiply(this.rate.add(shiftAbsolute)).stripTrailingZeros();

            // NEED SCALE for VALIDATE
            if (amountWant.scale() > this.wantAssetScale) {
                amountWant = amountWant.setScale(wantAssetScale, BigDecimal.ROUND_HALF_UP);
            }

        } else {
            haveKey = this.wantAssetKey;
            wantKey = this.haveAssetKey;

            amountWant = schemeAmount.negate().stripTrailingZeros();
            amountHave = amountWant.multiply(this.rate.subtract(shiftAbsolute)).stripTrailingZeros();

            // NEED SCALE for VALIDATE
            if (amountHave.scale() > this.wantAssetScale) {
                amountHave = amountHave.setScale(wantAssetScale, BigDecimal.ROUND_HALF_UP);
            }
        }

        return super.createOrder(schemeAmount, haveKey, wantKey, amountHave, amountWant);

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

        BigDecimal newRate = Rater.getRate(this.haveAssetKey, this.wantAssetKey, "wex");

        if (newRate == null) {
            // если курса нет то отменим все ордера и ждем
            LOGGER.info("Rate " + this.haveAssetName + "/" + this.wantAssetName +  " not found - clear all orders anr awaiting...");
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
