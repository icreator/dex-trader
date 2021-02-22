package org.erachain.dextrader.Raters;
// 30/03 ++

import org.erachain.dextrader.traders.TradersManager;

import java.math.BigDecimal;


public class RaterCross extends Rater {

    private String[] crossPath;
    private Long startKey;
    private Long endKey;

    /**
     * Задаем путь кросс-курса
     * ["12.95 polonex", "95.92 polonex", "92.14 binance", ... ]
     * @param tradersManager
     * @param sleepSec
     * @param name
     * @param crossPath
     */
    public RaterCross(TradersManager tradersManager, int sleepSec, String name, String[] crossPath) {
        super(tradersManager, name, name, null, sleepSec);

        this.crossPath = crossPath;
        startKey = Long.valueOf(crossPath[0].split("[\\.]")[0]);
        endKey = Long.valueOf(crossPath[crossPath.length - 1].split("[\\.]")[1].split("[ ]")[0]);

    }

    public void clearRates() {
        rates.remove(makeKey(startKey, endKey, this.courseName));
    }

    @Override
    protected void parse(String result) {
    }

    @Override
    public boolean tryGetRate() {

        // очистим сначала - может там ошибка опроса
        clearRates();

        try {
            // задержка для имитации опроса - ожидаем рейтеры другие пока они пробьются чтобы курс здесь повился
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            return true;
        }

        BigDecimal rate = BigDecimal.ONE;
        for (String path: crossPath) {
            BigDecimal ratePath = Rater.rates.get(path);
            if (ratePath == null)
                return false;

            rate = rate.multiply(ratePath);
        }

        rate = rate.setScale(12, BigDecimal.ROUND_HALF_DOWN);

        setRate(startKey, endKey, this.courseName, rate);

        return true;
    }
}
