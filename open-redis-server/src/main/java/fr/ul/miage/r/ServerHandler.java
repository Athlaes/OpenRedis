package fr.ul.miage.r;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandler extends Thread {
    private static final String UNKNOWN_KEY = "La clé n'est pas présente sur le serveur";
    public static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);
    private Socket socket;
    
    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String request = in.readLine();
    
            String response = this.manageMultipleRequest(request);
    
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

    private String manageMultipleRequest(String request) {
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

    private String addExpiration(String[] parsedRequest) {
        String res = "OK";
        final String key = parsedRequest[1];
        if (OpenServer.data.containsKey(key)) {
            try {
                OpenServer.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        OpenServer.data.remove(key);
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

    private String manageRequest(String request) {
        String res = "OK";
        String[] parsedRequest = request.split(" ");
        String command = parsedRequest[0];
        switch (command.toLowerCase()) {
            case "set":
                OpenServer.data.put(parsedRequest[1], parsedRequest[2]);
                break;
            case "get":
                if (OpenServer.data.containsKey(parsedRequest[1])) {
                    res = OpenServer.data.get(parsedRequest[1]);
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "strlen":
                if (OpenServer.data.containsKey(parsedRequest[1])) {
                    res = Integer.toString(OpenServer.data.get(parsedRequest[1]).length());
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "append":
                if (OpenServer.data.containsKey(parsedRequest[1])) {
                    OpenServer.data.replace(parsedRequest[1], OpenServer.data.get(parsedRequest[1])+parsedRequest[2]);
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "incr":
                if (OpenServer.data.containsKey(parsedRequest[1])) {
                    try {
                        int val = Integer.parseInt(OpenServer.data.get(parsedRequest[1]));
                        OpenServer.data.replace(parsedRequest[1], Integer.toString(val+1));
                    } catch (NumberFormatException e) {
                        res = "La valeur de cette clé n'est pas numérique";
                    }
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "decr":
                if (OpenServer.data.containsKey(parsedRequest[1])) {
                    try {
                        int val = Integer.parseInt(OpenServer.data.get(parsedRequest[1]));
                        OpenServer.data.replace(parsedRequest[1], Integer.toString(val-1));
                    } catch (NumberFormatException e) {
                        res = "La valeur de cette clé n'est pas numérique";
                    }
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "exists":
                if (OpenServer.data.containsKey(parsedRequest[1])) {
                    res = "true";
                } else {
                    res = "false";
                }
                break;
            case "del":
                if (OpenServer.data.containsKey(parsedRequest[1])) {
                    OpenServer.data.remove(parsedRequest[1]);
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "expire":
                res = this.addExpiration(parsedRequest);
                break;
            case "subscribe":
                break;
            default:
                res = "La commande n'a pas été comprise";
                break;
        }
        return res;
    }

    
}
