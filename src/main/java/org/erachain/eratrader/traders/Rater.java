package org.erachain.eratrader.traders;
// 30/03

import org.erachain.eratrader.controller.Controller;
import org.erachain.eratrader.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.TreeMap;


public abstract class Rater extends Thread {

    protected final Logger LOGGER;

    // HAVE KEY + WANT KEY + COURSE NAME
    private static HashMap<String, BigDecimal> rates = new HashMap<String, BigDecimal>();

    private TradersManager tradersManager;
    private long sleepTimestep;

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
        this.sleepTimestep = sleepSec * 1000;
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
        return rates.get(haveKey + "." + wantKey + " " + exchange);
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

        while (this.run) {

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                //FAILED TO SLEEP
            }

            try {
                this.tryGetRate();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            //SLEEP
            try {
                Thread.sleep(sleepTimestep);
            } catch (InterruptedException e) {
                //FAILED TO SLEEP
                break;
            }

        }

    }

    protected synchronized void setRate(Long haveKey, Long wantKey, String courseName, BigDecimal rate) {
            Rater.rates.put(haveKey + "." + wantKey + " " + courseName, rate);
            LOGGER.info("set RATE" + haveKey + "/" + wantKey + " on " + courseName + " = " + rate.toPlainString());
    }

    public void setRun(boolean status) {
        this.run = status;
    }

    public void close() {
        this.run = false;
    }
}
