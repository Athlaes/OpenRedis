package fr.ul.miage.r;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandler extends Thread {
    /**
     *
     */
    private static final String PARAM_UNCOMPLETE = "Nombre de paramètre insuffisant";
    private static final String UNKNOWN_KEY = "nil";
    public static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);

            String request = in.readLine();
    
            String response = this.manageMultipleRequest(request);
    
            // write response
            this.out.println(response);
    
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
        String res = "1";
        final String key = parsedRequest[1];
        if (parsedRequest.length >= 2 && OpenServer.data.containsKey(key)) {
            try {
                OpenServer.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        OpenServer.data.remove(key);
                    }
                }, Long.parseLong(parsedRequest[2]));
            } catch (NumberFormatException e) {
                res = "0";
            }
        } else {
            res = "0";
        }
        return res;
    }

    private String manageSubscription(String[] request){
        String res = "Subscription ended";
        String line;
        String channel = request[1];
        if (request.length == 2) {
            try {
                OpenServer.register(channel, this);
                this.out.println("Subscribe " + channel);
                while ((line = this.in.readLine()) != null) {
                    String[] parsedLine = line.split(" ");
                    if ("publish".equalsIgnoreCase(parsedLine[0]) && parsedLine.length == 3) {
                        OpenServer.sendMessage(parsedLine[1], parsedLine[2]);
                    }
                    if ("unsubscribe".equalsIgnoreCase(parsedLine[0])) {
                        return res;
                    }
                }
            } catch (IOException e) {
                return "Subscription interrupted";
            }
        } else {
            res = PARAM_UNCOMPLETE;
        }
        return res;
    }

    private String exists(String[] parsedRequest) {
        int res = 0;
        if (parsedRequest.length >= 2) {
            for(int i = 1; i < parsedRequest.length; i++) {
                if (OpenServer.data.containsKey(parsedRequest[i])) {
                    res++;
                }
            }
        }
        return Integer.toString(res);
    }

    private String deleteKeys(String[] parsedRequest){
        int res = 0;
        if (parsedRequest.length >= 2) {
            for(int i = 1; i < parsedRequest.length; i++){
                if (OpenServer.data.containsKey(parsedRequest[i])) {
                    OpenServer.data.remove(parsedRequest[i]);
                    res ++;
                }
            }
        }
        return Integer.toString(res);
    }

    private String manageRequest(String request) {
        String res = "OK";
        String[] parsedRequest = request.split(" ");
        String command = parsedRequest[0];
        switch (command.toLowerCase()) {
            case "set":
                if (parsedRequest.length == 3) {
                    OpenServer.data.put(parsedRequest[1], parsedRequest[2]);
                } else {
                    res = PARAM_UNCOMPLETE;
                }
                break;
            case "get":
                if (parsedRequest.length == 2 && OpenServer.data.containsKey(parsedRequest[1])) {
                    res = OpenServer.data.get(parsedRequest[1]);
                } else {
                    res = UNKNOWN_KEY;
                }
                break;
            case "strlen":
                if (parsedRequest.length == 2 && OpenServer.data.containsKey(parsedRequest[1])) {
                    res = Integer.toString(OpenServer.data.get(parsedRequest[1]).length());
                } else {
                    res = "0";
                }
                break;
            case "append":
                if (parsedRequest.length == 3) {
                    if(OpenServer.data.containsKey(parsedRequest[1])) {
                        OpenServer.data.replace(parsedRequest[1], OpenServer.data.get(parsedRequest[1])+parsedRequest[2]);
                    } else {
                        OpenServer.data.put(parsedRequest[1], parsedRequest[2]);
                    }
                    return OpenServer.data.get(parsedRequest[1]);
                }
                break;
            case "incr":
                if (parsedRequest.length == 2 && OpenServer.data.containsKey(parsedRequest[1])) {
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
                if (parsedRequest.length == 2 && OpenServer.data.containsKey(parsedRequest[1])) {
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
                res = this.exists(parsedRequest);
                break;
            case "del":
                res = this.deleteKeys(parsedRequest);
                break;
            case "expire":
                res = this.addExpiration(parsedRequest);
                break;
            case "subscribe":
                res = this.manageSubscription(parsedRequest);
                break;
            case "publish":
                if (parsedRequest.length == 3) {
                    res = OpenServer.sendMessage(parsedRequest[1], parsedRequest[2]);
                } else {
                    res = "0";
                }
                break;
            default:
                res = "La commande n'a pas été comprise";
                break;
        }
        return res;
    }

    public void receiveMessage(String channel, String message) {
        this.out.println(String.format("Canal : %s, Message : %s", channel, message));
    }
}
