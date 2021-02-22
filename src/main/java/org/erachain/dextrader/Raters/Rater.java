package org.erachain.dextrader.Raters;
// 30/03

import org.erachain.dextrader.controller.Controller;
import org.erachain.dextrader.api.CallRemoteApi;
import org.erachain.dextrader.traders.TradersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;


public abstract class Rater extends Thread {

    protected final Logger LOGGER;

    // HAVE KEY + WANT KEY + COURSE NAME
    protected static HashMap<String, BigDecimal> rates = new HashMap<String, BigDecimal>();

    private TradersManager tradersManager;
    private long sleepTimeStep;

    protected Controller cnt;
    protected CallRemoteApi caller;

    protected String courseName; // course name
    protected String apiURL;
    Map<String, String> headers;
    protected BigDecimal shiftRate = BigDecimal.ONE;
    private boolean run = true;


    public Rater(TradersManager tradersManager, String name, String courseName, String apiURL, int sleepSec) {

        this.setName(this.getClass().getName() + ": " + name);

        this.apiURL = apiURL;

        this.cnt = Controller.getInstance();
        this.caller = new CallRemoteApi();

        this.tradersManager = tradersManager;
        this.sleepTimeStep = sleepSec * 1000;
        this.courseName = courseName == null? name : courseName;

        LOGGER = LoggerFactory.getLogger(getName());

        this.start();
    }

    /**
     * clear on error
     */
    protected abstract void clearRates();

    protected abstract void parse(String result);

    public static HashMap<String, BigDecimal> getRates() {
        return rates;
    }

    public static BigDecimal getRate(long haveKey, long wantKey, String exchange) {
        return rates.get(makeKey(haveKey, wantKey, exchange));
    }

    public static void clearRates(long haveKey, long wantKey, String exchange) {
        rates.remove(makeKey(haveKey, wantKey, exchange));
    }

    public boolean tryGetRateSimple() {

        clearRates();

        try {
            URL url = new URL(this.apiURL);
            URLConnection uc = url.openConnection();
            uc.addRequestProperty("User-Agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            this.parse(uc.getContent().toString());
            errors = 0;
        } catch (Exception e) {
            if (errors == 0) {
                LOGGER.error(e.getMessage(), e);
            } else {
                LOGGER.error(e.getMessage());
            }
            errors++;
            return false;
        }

        return true;
    }

    int errors = 0;

    // {"success":true,"timestamp":1570239120,"date":"2019-10-05","base":"USD","rates":{"AED":3.6010391749342854,"AFN":76.7130739970982,"ALL":109.48629759616963,"AMD":466.9722785380982,"ANG":1.7213540645465177,"AOA":373.5804880200803,"ARS":56.517785577036605,"AUD":1.4478883181051785,"AWG":1.764636680914464,"AZN":1.671543272812857,"BAM":1.7455237049527677,"BBD":1.9803635151562498,"BDT":82.93743774753928,"BGN":1.7467001294066964,"BHD":0.36958354574330354,"BIF":1823.457903611607,"BMD":0.9803537116191963,"BND":1.352401866593482,"BOB":6.7822869691384815,"BRL":3.977396964824911,"BSD":0.9807752637151785,"BTN":69.45267244421784,"BWP":10.825105878200892,"BYN":2.030655660562321,"BYR":19214.932747735715,"BZD":1.9768871808949107,"CAD":1.305066467981607,"CDF":1632.2892847339285,"CHF":0.9757950668600891,"CLF":0.025414689620015175,"CLP":701.2508450649017,"CNH":7.00892857142857,"CNY":7.008062428924375,"COP":3366.5346457001783,"CRC":569.8649474530446,"CUC":0.9803537116191963,"CVE":98.86905807615177,"CZK":22.96581506607589,"DJF":174.2288478883214,"DKK":6.6686639739617855,"DOP":51.47237951452857,"DZD":117.85850848986607,"EEK":14.589285714285714,"EGP":15.985686835810712,"ERN":14.705656640916072,"ETB":29.07767538527857,"EUR":0.8928571428571428,"FJD":2.1617191482686606,"FKP":0.7969099251009731,"GBP":0.7948747107956518,"GEL":2.9067879690992853,"GGP":0.7948453001843035,"GHS":5.3040959178071425,"GIP":0.7969099251009731,"GMD":49.511639739617856,"GNF":9073.173949060714,"GTQ":7.625534292772857,"GYD":204.65377828320536,"HKD":7.685335869181606,"HNL":24.19551096035446,"HRK":6.627240108231071,"HTG":94.0438649464732,"HUF":296.9442766950357,"IDR":13856.319360025,"ILS":3.410850554880178,"IMP":0.7948453001843035,"INR":69.43359083957499,"IQD":1166.6209168267856,"ISK":121.35836437786605,"JEP":0.7948453001843035,"JMD":131.66523959844642,"JOD":0.6936041723853927,"JPY":104.82632053644642,"KES":101.86893161052677,"KGS":68.45369201207767,"KHR":4009.647024626518,"KMF":439.63999843143745,"KRW":1168.82708913375,"KWD":0.29822752048939283,"KYD":0.8173650052939107,"KZT":381.4952188149464,"LAK":8651.6218471825,"LBP":1481.8083398690178,"LKR":178.01756695816067,"LRD":206.41384847652677,"LSL":14.75469785498571,"LTL":2.894729618446339,"LVL":0.5930061566213125,"LYD":1.3872387357358926,"MAD":9.50000098035357,"MDL":17.360141759146426,"MGA":3566.0403101839283,"MKD":55.058663385749995,"MMK":1503.6178777302678,"MNT":2577.7150650954463,"MOP":7.922634406493839,"MRO":349.9866142504196,"MTL":0.6785714285714285,"MUR":35.7349113760241,"MVR":15.14683541821875,"MWK":716.6389014156339,"MXN":19.13184777067589,"MYR":4.103372416767947,"MZN":60.76269263950446,"NAD":14.754692953217857,"NGN":354.40152052860714,"NIO":33.01868260068214,"NOK":8.918130661542678,"NPR":111.12346182502678,"NZD":1.5495960942708034,"OMR":0.37749009842750886,"PAB":0.98078016548375,"PEN":3.312653425355893,"PGK":3.3185355476256246,"PHP":50.69948629465535,"PKR":152.93551429355358,"PLN":3.857107564409196,"PYG":6268.921193286517,"QAR":3.569505117446339,"RON":4.2392494411983925,"RSD":104.90801635230356,"RUB":63.39036116230714,"RWF":907.8075369593749,"SAR":3.6776498960824995,"SBD":8.11331320340375,"SCR":13.43186345633482,"SEK":9.650017646366964,"SGD":1.3518391435630357,"SHP":1.2949531390925892,"SLL":9264.34290714107,"SOS":568.6054840986607,"SRD":7.311515234696697,"SSP":130.08035714285714,"STD":21137.200501941068,"SVC":8.581432100701964,"SZL":14.754686090741965,"THB":29.82337751460714,"TJS":9.506101721500892,"TMT":3.431237990667053,"TND":2.8003842986549103,"TOP":2.282116387592678,"TRY":5.584294733539821,"TTD":6.637096584447678,"TWD":30.285124112779464,"TZS":2246.6801645033925,"UAH":24.110856437002674,"UGX":3609.221566801339,"UYU":36.47115603309642,"UZS":9264.342903219642,"VEF":9.791286616210714,"VES":7153.839285714285,"VND":22752.539116113392,"VUV":113.82447551076785,"WST":2.6066997372651786,"XAF":0.001708007864197066,"XAG":17.89135811130791,"XAU":1533.894736842115,"XCD":0.3774361251410705,"XDR":1.3951535360968164,"XOF":0.0016972369991968604,"XPF":0.009345273461479193,"XRH":5199.999999999889,"YER":245.4319085526071,"ZAR":14.74119054154732,"ZMK":8824.363351437232,"ZMW":12.892145406062498,"ZWL":315.6738961217232},"unit":"per ounce"}
    public boolean tryGetRate() {

        clearRates();

        String callerResult = null;
        try {
            callerResult = caller.ResponseValueAPI(this.apiURL, "GET", "", headers);
            this.parse(callerResult);
            errors = 0;
        } catch (Exception e) {
            if (errors == 0) {
                LOGGER.error(e.getMessage(), e);
            } else {
                LOGGER.error(e.getMessage());
            }
            errors++;
            return false;
        }

        return true;
    }

    public void run() {

        LOGGER.info("START");

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

        rates.put(makeKey(haveKey, wantKey, courseName), rate);
        // STORE BACK PRICE
        BigDecimal backRate = BigDecimal.ONE.divide(rate,12, BigDecimal.ROUND_HALF_UP);
        Rater.rates.put(makeKey(wantKey, haveKey, courseName), backRate);

        TradersManager.Pair<String, Integer> pair = tradersManager.getAsset(haveKey);
        if (pair == null) {
            LOGGER.info("set RATE " + "[" + haveKey + "] / " + "[" + wantKey + "] from " + courseName + " = " + rate.toPlainString());
            LOGGER.warn("asset [" + haveKey + "] not found...");
            return;
        }

        String haveName = pair.a;

        pair = tradersManager.getAsset(wantKey);
        if (pair == null) {
            LOGGER.info("set RATE " + "[" + haveKey + "] / " + "[" + wantKey + "] from " + courseName + " = " + rate.toPlainString());
            LOGGER.warn("asset [" + wantKey + "] not found...");
            return;
        }
        String wantName = pair.a;

        LOGGER.info("set RATE " + "[" + haveKey + "]" + haveName + " / " + "[" + wantKey + "]" + wantName + " from " + courseName + " = " + rate.toPlainString());

    }

    public void close() {
        this.run = false;
        interrupt();
        LOGGER.error("STOP:" + getName());
    }
}
