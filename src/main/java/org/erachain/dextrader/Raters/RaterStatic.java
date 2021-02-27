package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;

import java.math.BigDecimal;


public class RaterStatic extends Rater {

    public static String NAME = "static";

    private Long haveKey;
    private Long wantKey;
    private BigDecimal rate;

    public RaterStatic(TradersManager tradersManager, int sleepSec, long haveKey, long wantKey, BigDecimal rate) {
        super(tradersManager, NAME, NAME, null, sleepSec);

        this.haveKey = haveKey;
        this.wantKey = wantKey;
        this.rate = rate;

    }

    @Override
    public void clearRates() {
    }

    @Override
    protected void parse(String result) {
    }

    @Override
    public boolean tryGetRate() {
        setRate(haveKey, wantKey, this.courseName, rate);
        return true;
    }
}
