package fr.ul.miage.r;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class OpenServer
{
    /**
     *
     */
    private static final String UNKNOWN_KEY = "La clé n'est pas présente sur le serveur";

    public static final Logger logger = LoggerFactory.getLogger(OpenServer.class);

    //static ServerSocket variable
    private static ServerSocket server;
    //socket server port on which it will listen
    private static int port = 6379;

    private static Map<String, String> data = new HashMap<>();

    private static Timer timer = new Timer();
    
    public static void main( String[] args )
    {
        String request = "";
        //create the socket server object
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            logger.error("Impossible d'ouvrir le serveur sur le port 6379", e);
        }
        logger.info("Waiting for clients");
        //keep listens indefinitely until receives 'exit' call or program terminates
        while(!"exit".equalsIgnoreCase(request)){
            try {
                //creating socket and waiting for client connection
                Socket socket = server.accept();

                // read request 
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                request = in.readLine();

                String response = manageMultipleRequest(request);

                // write response
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(response);

                //close resources
                in.close();
                out.close();
                socket.close();
            } catch (Exception e) {
                logger.error("La requête n'a pas pu être traité correctement", e);
            }
        }
        logger.info("Shutting down Socket server!!");
        //close the ServerSocket object
        try {
            server.close();
        } catch (IOException e) {
            logger.error("Impossible d'éteindre le serveur socket", e);
        }
    }

    private static String manageMultipleRequest(String request) {
        String res = "";
        String[] multipleRequest = request.split("%n");
        if (multipleRequest.length == 1) {
            multipleRequest = request.split(">");
        }
        for (int i = 0; i < multipleRequest.length; i++) {
            String req = multipleRequest[i].replaceAll("^ ", "");
            res += manageRequest(req);
            res += (i == multipleRequest.length-1)?"":"\n";
        }
        logger.info("done.");
        return res;
    }

    private static String addExpiration(String[] parsedRequest) {
        String res = "OK";
        final String key = parsedRequest[1];
        if (data.containsKey(key)) {
            try {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        data.remove(key);
                    }
                }, Long.parseLong(parsedRequest[2]));
            } catch (NumberFormatException e) {
                res = "Impossible de récupérer la durée, la commande précédente n'a pas été exécuté";
            }
        } else {
            res = UNKNOWN_KEY;
        }
        return res;
    }

    private static String manageRequest(String request) {
        String res = "OK";
        String[] parsedRequest = request.split(" ");
        String command = parsedRequest[0];
        switch (command.toLowerCase()) {
            case "set":
                data.put(parsedRequest[1], parsedRequest[2]);
                break;
            case "get":
                if (data.containsKey(parsedRequest[1])) {
                    res = data.get(parsedRequest[1]);
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "strlen":
                if (data.containsKey(parsedRequest[1])) {
                    res = Integer.toString(data.get(parsedRequest[1]).length());
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "append":
                if (data.containsKey(parsedRequest[1])) {
                    data.replace(parsedRequest[1], data.get(parsedRequest[1])+parsedRequest[2]);
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "incr":
                if (data.containsKey(parsedRequest[1])) {
                    try {
                        int val = Integer.parseInt(data.get(parsedRequest[1]));
                        data.replace(parsedRequest[1], Integer.toString(val+1));
                    } catch (NumberFormatException e) {
                        res = "La valeur de cette clé n'est pas numérique";
                    }
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "decr":
                if (data.containsKey(parsedRequest[1])) {
                    try {
                        int val = Integer.parseInt(data.get(parsedRequest[1]));
                        data.replace(parsedRequest[1], Integer.toString(val-1));
                    } catch (NumberFormatException e) {
                        res = "La valeur de cette clé n'est pas numérique";
                    }
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "exists":
                if (data.containsKey(parsedRequest[1])) {
                    res = "true";
                } else {
                    res = "false";
                }
                break;
            case "del":
                if (data.containsKey(parsedRequest[1])) {
                    data.remove(parsedRequest[1]);
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "expire":
                res = addExpiration(parsedRequest);
                break;
            default:
                res = "La commande n'a pas été comprise";
                break;
        }
        return res;
    }
}
