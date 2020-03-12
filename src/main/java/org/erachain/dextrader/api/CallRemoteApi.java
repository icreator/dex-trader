package org.erachain.dextrader.api;

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

        URL obj = new URL(urlNode);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

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
        if (result.endsWith("null")) {
            return result.substring(0, result.length() - 4);
        }

        return response.toString();
    }
}
