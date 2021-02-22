package org.erachain.dextrader.api;

import org.erachain.dextrader.traders.TradersManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Class Call API for test
 */
public class CallRemoteApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallRemoteApi.class);

    /**
     * Call remote/local full node API for code request
     *
     * @param urlNode       set remote url full node
     * @param requestMethod request method (get, post, etc...)
     * @return number request answer
     */
    public String ResponseCodeAPI(String urlNode, String requestMethod)  {
      try {
          Integer result;

        URL obj = new URL(urlNode);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod(requestMethod.toUpperCase());
        result = con.getResponseCode();

        return result.toString();
      }
      catch (Exception e)
      {
          return "0";
      }
    }

    /**
     * Call remote/local full node API for check data request
     *
     * @param urlNode       set remote url full node
     * @param requestMethod request method (get, post, etc...)
     * @param value         is value for only "post" method
     * @param headers
     * @return data request answer
     * @throws Exception
     */
    public String ResponseValueAPI(String urlNode, String requestMethod, String value,
                                   Map<String, String> headers) throws Exception {

        LOGGER.debug(urlNode);

        URL obj = new URL(urlNode);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // protect from 403 error
        if (false) {
            con.addRequestProperty("User-Agent",
                    //"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0); ");
                    "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)");
        }

        con.setRequestMethod(requestMethod.toUpperCase());

        if (headers != null) {
            for (String key: headers.keySet()) {
                con.setRequestProperty(key, headers.get(key));
            }
        }

        switch (requestMethod.toUpperCase()) {
            case "GET":
                break;
            case "POST":
                con.setDoOutput(true);
                con.getOutputStream().write(value.getBytes(StandardCharsets.UTF_8));
                con.getOutputStream().flush();
                con.getOutputStream().close();
                break;
        }

        if (false) {
            String headerName = null;
            for (int i = 1; (headerName = con.getHeaderFieldKey(i)) != null; i++) {
                if (headerName.equals("Set-Cookie")) {
                    String cookie = con.getHeaderField(i);
                    LOGGER.debug(cookie);
                }
            }
        }

        if (false) {
            try {
                LOGGER.debug(con.getResponseMessage());
                LOGGER.debug("" + con.getResponseCode());
            } catch (Exception e) {
            }
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            response.append(in.readLine());
        }
        in.close();
        String result = response.toString();

        if (false) {
            LOGGER.debug(result);
        }

        if (result.endsWith("null")) {
            return result.substring(0, result.length() - 4);
        }

        return result;
    }
}
