package org.erachain.dextrader.api;
// 30/03

import org.erachain.dextrader.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiClient.class);

    String[][] helpStrings =
            {
                    {
                            "GET core/stop",
                            "Will stop the application. This command might not be able to return a http OK message.",
                            ""
                    },
                    {
                            "GET core/status",
                            "Returns the status of the application.",
                            "0 - No connections. 1 - Synchronizing 2 - OK"
                    },

            };


    /**
     *
     * @param command send command
     * @return result execute command or error
     */
    public String executeCommand(String command) {
        if (command.toLowerCase().equals("help all")) {
            String help = "\n";

            for (String[] strings : helpStrings) {
                help += strings[0] + "\n\t" + strings[1] + "\n\n";
            }

            return help;
        }

        if (command.toLowerCase().startsWith("help")) {
            command = command.substring(4, command.length()).toLowerCase();
            String[] args = command.split("[ /<>]");

            String help = "";

            if (args.length > 1) {
                boolean found = false;
                for (String[] helpString : helpStrings) {

                    String[] helparray = helpString[0].toLowerCase().split("[ /<>]");

                    boolean notallfound = false;
                    for (String string : args) {
                        if (string.equals("")) {
                            continue;
                        }

                        if (Arrays.asList(helparray).indexOf(string) == -1) {
                            notallfound = true;
                            break;
                        }
                    }

                    if (!notallfound) {
                        help += helpString[0] + "\n\t" + helpString[1] + "\n\t" + helpString[2] + "\n\n";
                        found = true;
                        if (helparray.length == args.length - 1) {
                            break;
                        }
                    }
                }
                if (!found) {
                    help += "Command not found!\n";

                }
            } else {
                help = "\n";
                for (String[] helpString : helpStrings) {
                    help += helpString[0] + "\n";
                }
            }

            help += "\nType \"help all\" for detailed help for all commands. Or type \"help command\" to get detailed help for that command. Type \"clear\" for clear GUI concole.\n";

            return help;
        }

        try {
            //SPLIT
            String[] args = command.split(" ");

            //GET METHOD
            String method = args[0].toUpperCase();

            //GET PATH
            String path;

            //GET CONTENT
            String content = "";
            String vars = "";

            if (method.equals("POST")) {
                path =  args[1];
                content = command.substring((method + " " + path + " ").length());
            } else {
                path = command.substring((method + " ").length());

                // get telegrams/address?erty=132 123&sdf=вва
                int startVars = command.indexOf("?");
                if (startVars > 0) {
                    content = command.substring(startVars + 1);
                    path = command.substring((method + " ").length(), startVars);
                    String[] items = content.split("&");

                    for (String item : items) {
                        String[] value = item.split("=");
                        if (value.length > 1) {
                            vars += URLEncoder.encode(value[0], "UTF-8") + "=" + URLEncoder.encode(value[1], "UTF-8") + "&";
                        } else {
                            vars += URLEncoder.encode(item, "UTF-8") + "&";
                        }
                    }
                    vars = vars.substring(0, vars.length() - 1);

                }
            }

            //URL CANNOT CONTAIN UNICODE CHARACTERS
            String[] paths = path.split("/");
            String path2 = "";
            for (String string : paths) {
                path2 += URLEncoder.encode(string, "UTF-8") + "/";
            }
            path2 = path2.substring(0, path2.length() - 1);

            if (vars.length() > 0) {
                path2 += "?" + vars;
            }

            //CREATE CONNECTION
            URL url = new URL("http://127.0.0.1:" + Settings.getInstance().getRpcPort() + "/" + path2);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            //EXECUTE
            connection.setRequestMethod(method);

            if (method.equals("POST")) {
                connection.setDoOutput(true);
                connection.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
                connection.getOutputStream().flush();
                connection.getOutputStream().close();
            }

            //READ RESULT
            InputStream stream;
            if (connection.getResponseCode() == 400) {
                stream = connection.getErrorStream();
            } else {
                stream = connection.getInputStream();
            }

            InputStreamReader isReader = new InputStreamReader(stream, "UTF-8");
            BufferedReader br = new BufferedReader(isReader);
            String result = br.readLine(); //TODO READ ALL OR HARDCODE HELP


            try {
                return result.toString();
            } catch (Exception e) {
                return result;
            }

        } catch (Exception ioe) {
            LOGGER.info(ioe.getMessage());
            return "Invalid command! \n" +
                    ioe.getMessage() + "\n" +
                    "Type help to get a list of commands. ";
        }
    }


}
