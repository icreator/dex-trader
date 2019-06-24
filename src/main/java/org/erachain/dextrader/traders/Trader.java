package org.erachain.dextrader.traders;

import org.erachain.dextrader.Raters.Rater;
import org.erachain.dextrader.api.CallRemoteApi;
import org.erachain.dextrader.controller.Controller;
import org.erachain.dextrader.core.transaction.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public abstract class Trader extends Thread {

    protected Logger LOGGER;

    private static final int INVALID_TIMESTAMP = 7;
    private static final int ORDER_DOES_NOT_EXIST = 36;

    protected static final BigDecimal M100 = new BigDecimal(100).setScale(0);

    private TradersManager tradersManager;
    protected long sleepTimestep;

    protected Controller cnt;
    protected CallRemoteApi caller;

    protected String sourceExchange;
    protected boolean cleanAllOnStart;
    protected String address;

    protected BigDecimal shiftRate = BigDecimal.ONE;
    protected long haveAssetKey;
    protected long wantAssetKey;
    protected String haveAssetName;
    protected String wantAssetName;
    protected int wantAssetScale;

    protected BigDecimal limitUP;
    protected BigDecimal limitDown;

    protected long startDelay;

    protected BigDecimal rate;

    protected static final int STATUS_INCHAIN = 2;
    protected static final int STATUS_UNFCONFIRMED = -1;

    // KEY -> ORDER
    //protected TreeSet<BigInteger> orders = new TreeSet<>();

    // AMOUNT + SPREAD
    protected HashMap<BigDecimal, BigDecimal> scheme;

    // AMOUNT -> Tree Map of (ORDER.Tuple3 + his STATUS)
    protected HashMap<BigDecimal, HashSet<String>> schemeOrders = new HashMap();

    // AMOUNT -> Tree Set of SIGNATURE
    protected HashMap<BigDecimal, HashSet<String>> unconfirmedsCancel = new HashMap();

    private boolean run = true;

    public Trader(TradersManager tradersManager, String accountStr, int sleepSec,
                  String sourceExchange, HashMap<BigDecimal, BigDecimal> scheme, Long haveKey, Long wantKey,
                  boolean cleanAllOnStart, BigDecimal limitUP, BigDecimal limitDown) {

        LOGGER = LoggerFactory.getLogger(this.getClass().getSimpleName());

        this.cnt = Controller.getInstance();
        this.caller = new CallRemoteApi();

        this.sourceExchange = sourceExchange;
        this.cleanAllOnStart = cleanAllOnStart;
        this.address = accountStr;
        this.tradersManager = tradersManager;
        this.sleepTimestep = sleepSec * 1000;

        this.scheme = scheme;

        this.haveAssetKey = haveKey;
        this.wantAssetKey = wantKey;

        this.haveAssetName = "haveA";
        this.wantAssetName = "wantA";

        this.limitUP = limitUP;
        this.limitDown = limitDown;

        // IF that TRANSACTION exist in CHAIN or queue
        TradersManager.Pair pair = tradersManager.getAsset(haveAssetKey);
        if (pair == null)
            return;
        haveAssetName = (String)pair.a;

        pair = tradersManager.getAsset(wantAssetKey);
        if (pair == null)
            return;
        wantAssetName = (String)pair.a;
        wantAssetScale = (int)pair.b;


        this.setName("Trader - " + this.getClass().getSimpleName() + " pair: [" + haveAssetKey + "]" + haveAssetName
                + " / [" + wantAssetKey + "]" + wantAssetName + " @" + sourceExchange + " on " + address);

        this.start();
    }

    public Trader(TradersManager tradersManager, String accountStr, JSONObject json) {

        LOGGER = LoggerFactory.getLogger(this.getClass().getSimpleName());

        this.cnt = Controller.getInstance();
        this.caller = new CallRemoteApi();

        try {
            cleanAllOnStart = (boolean) json.get("cleanAllOnStart");
        } catch (Exception e) {

        }

        this.sourceExchange = json.get("sourceExchange").toString();

        this.address = accountStr;
        this.tradersManager = tradersManager;
        this.sleepTimestep = ((int) (long) json.get("sleepTime")) * 1000;

        if (json.containsKey("scheme")) {
            scheme = new HashMap<>();
            JSONObject schemeJSON = (JSONObject) json.get("scheme");
            for (Object key : schemeJSON.keySet()) {
                scheme.put(new BigDecimal(key.toString()),
                        new BigDecimal(schemeJSON.get(key).toString()));
            }
        }

        this.haveAssetKey = (long) json.get("haveAssetKey");
        this.wantAssetKey = (long) json.get("wantAssetKey");

        this.haveAssetName = "haveA";
        this.wantAssetName = "wantA";

        try {
            this.limitUP = new BigDecimal(json.get("limitUP").toString());
            this.limitDown = new BigDecimal(json.get("limitDown").toString());
        } catch (Exception e) {

        }

        // IF that TRANSACTION exist in CHAIN or queue
        TradersManager.Pair pair = tradersManager.getAsset(haveAssetKey);
        if (pair == null)
            return;
        haveAssetName = (String)pair.a;

        pair = tradersManager.getAsset(wantAssetKey);
        if (pair == null)
            return;
        wantAssetName = (String)pair.a;
        wantAssetScale = (int)pair.b;

        try {
            startDelay = (long) json.get("startDelay");
        } catch (Exception e) {

        }

        this.setName("Trader - " + this.getClass().getSimpleName() + " pair: [" + haveAssetKey + "]" + haveAssetName
                + " / [" + wantAssetKey + "]" + wantAssetName + " @" + sourceExchange + " on " + address);

        this.start();

    }

    protected synchronized void schemeOrdersPut(BigDecimal amount, String orderID) {
        HashSet<String> set = schemeOrders.get(amount);
        if (set == null)
            set = new HashSet();

        set.add(orderID);
        schemeOrders.put(amount, set);
    }

    protected synchronized boolean schemeOrdersRemove(BigDecimal amount, String orderID) {
        HashSet<String> set = schemeOrders.get(amount);

        if (set == null || set.isEmpty())
            return false;

        boolean removed = set.remove(orderID);
        schemeOrders.put(amount, set);
        return removed;
    }

    /*
    protected synchronized void unconfirmedsCancelPut(BigDecimal amount, String signatire) {
        TreeSet<String> treeSet = unconfirmedsCancel.get(amount);
        if (treeSet == null)
            treeSet = new TreeSet();

        treeSet.add(signatire);
        unconfirmedsCancel.put(amount, treeSet);
    }

    protected synchronized boolean unconfirmedsCancelRemove(BigDecimal amount, String signature) {
        TreeSet<String> treeSet = unconfirmedsCancel.get(amount);
        boolean removed = treeSet.remove(signature);
        unconfirmedsCancel.put(amount, treeSet);
        return removed;
    }
    */

    protected boolean createOrder(BigDecimal schemeAmount,
                                  Long haveKey, String haveName, BigDecimal amountHave,
                                  Long wantKey, String wantName, BigDecimal amountWant) {

        String result;

        LOGGER.info("TRY CREATE " + haveName + "/" + wantName + " : " + amountHave.toPlainString() + " -> " + amountWant.toPlainString());

        JSONObject jsonObject = null;
        // TRY MAKE ORDER in LOOP
        do {

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return true;
            }

            String urlCommand = "GET trade/create/" + this.address + "/" + haveKey + "/" + wantKey
                    + "/" + amountHave.toPlainString() + "/" + amountWant.toPlainString();
            result = cnt.apiClient.executeCommand(urlCommand + "?password=" + TradersManager.WALLET_PASSWORD);

            try {
                //READ JSON
                jsonObject = (JSONObject) JSONValue.parse(result);
            } catch (NullPointerException | ClassCastException e) {
                //JSON EXCEPTION
                LOGGER.error(e.getMessage());
            } finally {
                cnt.apiClient.executeCommand("GET wallet/lock");
            }

            if (jsonObject == null)
                return false;

            if (jsonObject.containsKey("signature")) {
                break;
            }

            int error = ((Long)jsonObject.get("error")).intValue();

            if (error == INVALID_TIMESTAMP) {
                // INVALIT TIMESTAMP
                continue;
            }

            //LOGGER.error("CREATE: " + urlCommand);
            LOGGER.error("MESS: " + result);
            return false;

        } while (true);

        this.schemeOrdersPut(schemeAmount, jsonObject.get("signature").toString());
        return true;

    }

    protected boolean cancelOrder(String orderSignature) {

        String result;

        result = cnt.apiClient.executeCommand("GET trade/get/" + orderSignature);
        //logger.info("GET: " + Base58.encode(orderID) + "\n" + result);

        JSONObject jsonObject = null;
        try {
            //READ JSON
            jsonObject = (JSONObject) JSONValue.parse(result);
        } catch (NullPointerException | ClassCastException e) {
            //JSON EXCEPTION
            LOGGER.error(e.getMessage());
            //throw ApiErrorFactory.getInstance().createError(ApiErrorFactory.ERROR_JSON);
        }
        if (!jsonObject.containsKey("id")
                || !jsonObject.containsKey("active")) {
            return false;
        }

        jsonObject = null;

        do {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //FAILED TO SLEEP
                return true;
            }

            result = cnt.apiClient.executeCommand("GET trade/cancel/" + this.address + "/" + orderSignature
                    + "?password=" + TradersManager.WALLET_PASSWORD);

            try {
                //READ JSON
                jsonObject = (JSONObject) JSONValue.parse(result);
            } catch (NullPointerException | ClassCastException e) {
                //JSON EXCEPTION
                LOGGER.error(e.getMessage());
                //throw ApiErrorFactory.getInstance().createError(ApiErrorFactory.ERROR_JSON);
            } finally {
                cnt.apiClient.executeCommand("GET wallet/lock");
            }

            if (jsonObject == null)
                return false;

            if (jsonObject.containsKey("signature"))
                break;

            int error = ((Long) jsonObject.get("error")).intValue();
            if (error == INVALID_TIMESTAMP) {
                // INVALIT TIMESTAMP
                continue;
            } else if (error == ORDER_DOES_NOT_EXIST) {
                // ORDER not EXIST - as DELETED
                return true;
            }

            LOGGER.info("CANCEL: " + orderSignature + "\n" + result);
            return false;

        } while(true);

        return true;

    }

    protected JSONArray getMyOrders(String address, long haveKey, long wantKey) {

        JSONArray result = new JSONArray();

        String sendRequest;

        JSONParser jsonParser = new JSONParser();
        try {
            sendRequest = cnt.apiClient.executeCommand("GET trade/getbyaddress/" + address
                    + '/' + haveKey + '/' + wantKey);
            //READ JSON
            result = (JSONArray) jsonParser.parse(sendRequest);
        } catch (NullPointerException | ClassCastException | ParseException e) {
            //JSON EXCEPTION
            LOGGER.error(e.getMessage());
        }

        return result;

    }

    protected HashSet<String> makeCancelingArray(JSONArray array) {

        HashSet<String> cancelingArray = new HashSet();
        if (array == null || array.isEmpty())
            return cancelingArray;

        String result;
        JSONObject transaction;
        for (int i=0; i < array.size(); i++) {
            JSONObject transactionJSON = (JSONObject) array.get(i);

            // IF that TRANSACTION exist in CHAIN or queue
            result = cnt.apiClient.executeCommand("GET transactions/signature/" + transactionJSON.get("signature"));
            try {
                //READ JSON
                transaction = (JSONObject) JSONValue.parse(result);
            } catch (NullPointerException | ClassCastException e) {
                //JSON EXCEPTION
                LOGGER.error(e.getMessage());
                break;
            }

            if (transaction == null || !transaction.containsKey("signature"))
                continue;

            if ((int) (long) transaction.get("type") == Transaction.CANCEL_ORDER_TRANSACTION) {
                if (true //// || (long)transaction.get("timestamp") > transaction.getCreator().getLastTimestamp()
                        ) {
                    cancelingArray.add(transaction.get("orderSignature").toString());
                }
            }
        }

        return cancelingArray;

    }

    // REMOVE ALL ORDERS
    protected void removaAll() {

        String result = cnt.apiClient.executeCommand("GET transactions/unconfirmedof/" + this.address);

        JSONArray arrayUnconfirmed = null;
        try {
            //READ JSON
            arrayUnconfirmed = (JSONArray) JSONValue.parse(result);
        } catch (NullPointerException | ClassCastException e) {
            //JSON EXCEPTION
            LOGGER.error(e.getMessage());
        }

        BigDecimal amount;
        String orderSignature;
        HashSet<String> cancelsIsUnconfirmed;
        boolean updated = false;

        if (arrayUnconfirmed != null) {

            // GET CANCELS in UNCONFIRMEDs
            cancelsIsUnconfirmed = makeCancelingArray(arrayUnconfirmed);
            if (cancelsIsUnconfirmed == null)
                return;

            // CHECK MY ORDERs in UNCONFIRMED
            for (Object json : arrayUnconfirmed) {

                JSONObject transaction = (JSONObject) json;
                if (((int)(long) transaction.get("type")) == Transaction.CREATE_ORDER_TRANSACTION) {
                    Object haveKeyObj = transaction.get("haveAssetKey");
                    Object wantKeyObj = transaction.get("wantAssetKey");
                    if (haveKeyObj != null && wantKeyObj != null && (
                            haveKeyObj.equals(this.haveAssetKey)
                                && wantKeyObj.equals(this.wantAssetKey)
                            || haveKeyObj.equals(this.wantAssetKey)
                                && wantKeyObj.equals(this.wantAssetKey))
                    ) {

                        orderSignature = (String) transaction.get("signature");
                        // IF not aldeady CANCEL in WAITING
                        if (cancelsIsUnconfirmed != null && cancelsIsUnconfirmed.contains(orderSignature))
                            continue;

                        // CANCEL ORDER
                        if (cancelOrder(orderSignature) && !updated)
                            updated = true;

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            //FAILED TO SLEEP
                            return;
                        }
                    }
                }
            }
        } else {
            cancelsIsUnconfirmed = null;
        }

        // CHECK MY SELL ORDERS in CAP
        JSONArray list =  this.getMyOrders(this.address, this.haveAssetKey, this.wantAssetKey);
        if (list != null) {

            for (Object item : list) {

                JSONObject order = (JSONObject) item;
                if (!order.containsKey("signature"))
                    continue;

                orderSignature = (String) order.get("signature");
                if (cancelsIsUnconfirmed != null && cancelsIsUnconfirmed.contains(orderSignature))
                    continue;

                if (this.scheme.containsKey(orderSignature)) {
                    schemeOrdersRemove(new BigDecimal(order.get("amountHave").toString()), orderSignature);
                }

                // CANCEL ORDER
                if (cancelOrder(orderSignature) && !updated)
                    updated = true;

            }
        }

        // CHECK MY BUY ORDERS in CAP
        list = this.getMyOrders(this.address, this.wantAssetKey, this.haveAssetKey);
        if (list != null)
            for (Object item: list) {

                JSONObject order = (JSONObject) item;
                if (!order.containsKey("signature"))
                continue;

                orderSignature = (String) order.get("signature");
                if (cancelsIsUnconfirmed != null && cancelsIsUnconfirmed.contains(orderSignature))
                    continue;

                if (this.scheme.containsKey(orderSignature)) {
                    schemeOrdersRemove(new BigDecimal(order.get("amountWant").toString()).negate(), orderSignature);
                }

                // CANCEL ORDER
                if(cancelOrder(orderSignature) && !updated)
                    updated = true;

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //FAILED TO SLEEP
                    return;
                }

            }

        if (updated) {
            try {
                Thread.sleep(Controller.GENERATING_MIN_BLOCK_TIME_MS << 1);
            } catch (InterruptedException e) {
                //FAILED TO SLEEP
                return;
            }
        }

    }

    // REMOVE ALL ORDERS
    protected boolean cleanSchemeOrders() {

        boolean cleaned = false;

        if (this.schemeOrders == null || this.schemeOrders.isEmpty())
            return cleaned;

        // CANCEL ALL MY ORDERS in UNCONFIRMED

        BigDecimal amount;
        String result;
        JSONObject transaction = null;
        for (BigDecimal amountKey: this.schemeOrders.keySet()) {

            HashSet<String> schemeItems = this.schemeOrders.get(amountKey);

            if (schemeItems == null || schemeItems.isEmpty())
                continue;

            for (String orderID: schemeItems) {

                // IF that TRANSACTION exist in CHAIN or queue
                result = cnt.apiClient.executeCommand("GET transactions/signature/" + orderID);
                try {
                    //READ JSON
                    transaction = (JSONObject) JSONValue.parse(result);
                } catch (NullPointerException | ClassCastException e) {
                    //JSON EXCEPTION
                    LOGGER.error(e.getMessage());
                }

                if (transaction == null || !transaction.containsKey("signature"))
                    continue;

                cleaned = cancelOrder(orderID);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //FAILED TO SLEEP
                    return false;
                }
            }

            // CLEAR map
            schemeItems.clear();
            this.schemeOrders.put(amountKey, schemeItems);
        }

        // CLEAR cancels
        unconfirmedsCancel.clear();
        return cleaned;
    }

    public boolean updateCap() {

        String result;
        boolean updated = false;

        for (BigDecimal schemeAmount: this.schemeOrders.keySet()) {

            HashSet<String> schemeItems = this.schemeOrders.get(schemeAmount);

            if (schemeItems == null || schemeItems.isEmpty())
                continue;

            // make copy of LIST - for concerent DELETE
            for (String orderID: new ArrayList<>(schemeItems)) {
                result = cnt.apiClient.executeCommand("GET trade/get/" + orderID);
                //logger.info("GET: " + Base58.encode(orderID) + "\n" + result);

                JSONObject jsonObject = null;
                try {
                    //READ JSON
                    jsonObject = (JSONObject) JSONValue.parse(result);
                } catch (NullPointerException | ClassCastException e) {
                    //JSON EXCEPTION
                    LOGGER.error(e.getMessage());
                    //throw ApiErrorFactory.getInstance().createError(ApiErrorFactory.ERROR_JSON);
                }

                if (jsonObject != null && (jsonObject.containsKey("completed") || jsonObject.containsKey("canceled"))) {

                    // remake Order if it COMPLETED
                    boolean created = false;

                    if (schemeAmount.signum() > 0) {
                        created = createOrder(schemeAmount, haveAssetKey, haveAssetName, new BigDecimal(jsonObject.get("amountHave").toString()),
                                wantAssetKey, wantAssetName, new BigDecimal(jsonObject.get("amountWant").toString()));
                    } else {
                        // в другую сторону
                        created = createOrder(schemeAmount, wantAssetKey, wantAssetName, new BigDecimal(jsonObject.get("amountHave").toString()),
                                haveAssetKey, haveAssetName, new BigDecimal(jsonObject.get("amountWant").toString()));
                    }

                    if (created) {
                        // если был создан новый ордер то сслыку на старый нужно удалить - чтобы еще раз не создавать потом
                        schemeOrdersRemove(schemeAmount, orderID);
                    }

                    updated = true;
                }
            }

        }

        return updated;
    }

    protected abstract boolean process();

    public void run() {

        // можно отключить при отладке
        boolean removaAllOn = true;

        LOGGER.info("START " + this.getName());
        // WAIT START WALLET
        // IF WALLET NOT ESXST - suspended
        while(cnt.getStatus() == 0 || Rater.getRate(this.haveAssetKey, this.wantAssetKey, sourceExchange) == null) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }

        }

        if (cleanAllOnStart && removaAllOn) {
            removaAll();
        }

        if (startDelay > 0) {
            try {
                Thread.sleep(startDelay * 1000);
            } catch (InterruptedException e) {
                //FAILED TO SLEEP
                return;
            }
        }

        while (!isInterrupted() && !cnt.isOnStopping() && this.run) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //FAILED TO SLEEP
                break;
            }

            if (cnt.getStatus() == 0 ||
                    !this.run) {
                continue;
            }

            try {

                this.process();

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

        // ON EXIT remove all
        if (removaAllOn) {
            cleanSchemeOrders();
        }

    }

    public void close() {
        this.run = false;
        interrupt();
        LOGGER.error("STOP:" + getName());
    }
}
