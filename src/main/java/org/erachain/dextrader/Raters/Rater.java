package org.erachain.dextrader.Raters;
// 30/03

import org.erachain.dextrader.controller.Controller;
import org.erachain.dextrader.api.CallRemoteApi;
import org.erachain.dextrader.traders.TradersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;


public abstract class Rater extends Thread {

    protected final Logger LOGGER;

    // HAVE KEY + WANT KEY + COURSE NAME
    protected static HashMap<String, BigDecimal> rates = new HashMap<String, BigDecimal>();

    private TradersManager tradersManager;
    private long sleepTimeStep;

    protected Controller cnt;
    protected CallRemoteApi caller;

    // https://api.livecoin.net/exchange/ticker?currencyPair=EMC/BTC
    // https://poloniex.com/public?command=returnTradeHistory&currencyPair=BTC_DOGE
    // https://wex.nz/api/3/ticker/btc_rur
    protected String courseName; // course name
    protected String apiURL;
    protected BigDecimal shiftRate = BigDecimal.ONE;
    private boolean run = true;


    public Rater(TradersManager tradersManager, String courseName, int sleepSec) {

        LOGGER = LoggerFactory.getLogger(this.getClass().getName());

        this.cnt = Controller.getInstance();
        this.caller = new CallRemoteApi();

        this.tradersManager = tradersManager;
        this.sleepTimeStep = sleepSec * 1000;
        this.courseName = courseName;

        this.setName("Thread Rater - " + this.getClass().getName() + ": " + this.courseName);
        LOGGER.info("start RATER" + this.courseName);

        this.start();
    }

    protected abstract void parse(String result);

    public static HashMap<String, BigDecimal> getRates() {
        return rates;
    }

    public static BigDecimal getRate(long haveKey, long wantKey, String exchange) {
        return rates.get(makeKey(haveKey, wantKey, exchange));
    }

    public boolean tryGetRate() {

        String callerResult = null;
        try {
            callerResult = caller.ResponseValueAPI(this.apiURL, "GET", "");
            this.parse(callerResult);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    public void run() {

        while (!isInterrupted() && !cnt.isOnStopping() && this.run) {

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                //FAILED TO SLEEP
                break;
            }

            try {
                this.tryGetRate();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            //SLEEP
            try {
                Thread.sleep(sleepTimeStep);
            } catch (InterruptedException e) {
                //FAILED TO SLEEP
                break;
            }

        }

    }

    public static String makeKey(Long haveKey, Long wantKey, String courseName) {
        return haveKey + "." + wantKey + " " + courseName;
    }

    protected synchronized void setRate(Long haveKey, Long wantKey, String courseName, BigDecimal rate) {
        Rater.rates.put(makeKey(haveKey, wantKey, courseName), rate);

        TradersManager.Pair<String, Integer> pair = tradersManager.getAsset(haveKey);
        if (pair == null)
            return;
        String haveName = pair.a;

        pair = tradersManager.getAsset(wantKey);
        if (pair == null)
            return;
        String wantName = pair.a;

        // STORE BACK PRICE
        BigDecimal backRate = BigDecimal.ONE.divide(rate,12, BigDecimal.ROUND_HALF_UP);
        Rater.rates.put(makeKey(wantKey, haveKey, courseName), backRate);

        LOGGER.info("set RATE " + "[" + haveKey + "]" + haveName + " / " + "[" + wantKey + "]" + wantName + " on " + courseName + " = " + rate.toPlainString());
    }

    public void close() {
        this.run = false;
        interrupt();
        LOGGER.error("STOP:" + getName());
    }
}
