package fr.ul.miage.r;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                request = (String) ois.readObject();

                String response = manageRequest(request);

                // write response
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(response);

                //close resources
                ois.close();
                oos.close();
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

    private static String manageRequest(String request) {
        String res = "OK";
        String[] parsedRequest = request.split(" ");
        String command = parsedRequest[0];
        if (!command.isEmpty()) {
            switch (command.toLowerCase()) {
                case "set":
                    data.put(res, command);
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
                        res = Integer.toString(data.get(parsedRequest[1]).length());
                    } else {
                        res = UNKNOWN_KEY;
                    }
                    break;
                case "incr":
                    if (data.containsKey(parsedRequest[1])) {
                        int val = Integer.parseInt(data.get(parsedRequest[1]));
                        data.replace(parsedRequest[1], Integer.toString(val+1));
                    } else {
                        res = UNKNOWN_KEY;
                    }
                    break;
                case "decr":
                    if (data.containsKey(parsedRequest[1])) {
                        int val = Integer.parseInt(data.get(parsedRequest[1]));
                        data.replace(parsedRequest[1], Integer.toString(val-1));
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
            res = "La clé n'a pas été trouvé";
        }
        return res;
    }
}
