package fr.ul.miage.r;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            Object response = this.manageMultipleRequest(request);
    
            // write response
            this.out.println(response.toString());
    
            //close resources
            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            logger.error("La requête n'a pas pu être traité correctement", e);
        }
    }

    /***
     * Permet de récupérer la requête string et de la parser en fonction du nombre de ligne ou de la présence de pipeline. 
     * Chaque requête trouvée est ensuite traitée indivuellement à la suite les unes des autres. Si il n'y en a qu'une, elle est la seule à être exécutée.
     * @param request la requête String
     * @return la réponse String
     */
    private String manageMultipleRequest(String request) {
        String res = "";
        String[] multipleRequest = request.split("%n");
        if (multipleRequest.length == 1) {
            multipleRequest = request.split(">");
        }
        for (int i = 0; i < multipleRequest.length; i++) {
            String req = multipleRequest[i].replaceAll("^ ", "");
            Object objRes = manageRequest(req);
            res += objRes instanceof Integer ? "(integer) " + objRes.toString() : "\"" + objRes.toString() + "\"" ; 
            res += (i == multipleRequest.length-1)?"":"\n";
        }
        logger.info("done.");
        return res;
    }

    /***
     * Ajoute une expiration en seconde à une clé dans la map data du serveur.
     * @param request la requête est passé en argument pour être envoyé vers les esclaves après vérification de la validité de la requête
     * @param parsedRequest
     * @return 1 si la requête s'est exécutée 0 sinon
     */
    private int addExpiration(String request, String[] parsedRequest) {
        int res = 1;
        final String key = parsedRequest[1];
        if (parsedRequest.length >= 2 && OpenServer.data.containsKey(key)) {
            try {
                OpenServer.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        OpenServer.data.remove(key);
                    }
                }, Long.parseLong(parsedRequest[2])*1000);
            } catch (NumberFormatException e) {
                res = 0;
            }
            OpenServer.sendRequestToSlaves(request);
        } else {
            res = 0;
        }
        return res;
    }

    /**
     * Permet d'ouvrir une subscription
     * Lorsque la souscription s'ouvre on bloque l'exécution du Thread avec un while tant que le l'input stream de la socket n'est pas fermé pour garder la socket ouverte tant 
     * que le client en a besoin. On peut alors lire les requêtes envoyés par le client dans la même socket. 
     * @param request 
     * @return la réponse String
     */
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

    /***
     * Permet de trouver les variables existantes dans la map data
     * @param parsedRequest
     * @return le nombre de variable existantes
     */
    private int exists(String[] parsedRequest) {
        int res = 0;
        if (parsedRequest.length >= 2) {
            for(int i = 1; i < parsedRequest.length; i++) {
                if (OpenServer.data.containsKey(parsedRequest[i])) {
                    res++;
                }
            }
        }
        return res;
    }

    /***
     * Permet de supprimer des clés de la map data du serveur
     * @param request la requête à faire poursuivre aux esclaves si elle est valide
     * @param parsedRequest la requête parsé
     * @return le nombre de clés supprimés
     */
    private int deleteKeys(String request, String[] parsedRequest){
        int res = 0;
        if (parsedRequest.length >= 2) {
            for(int i = 1; i < parsedRequest.length; i++){
                if (OpenServer.data.containsKey(parsedRequest[i])) {
                    OpenServer.data.remove(parsedRequest[i]);
                    OpenServer.sendRequestToSlaves(request);
                    res ++;
                }
            }
        }
        return res;
    }

    /**
     * Permet de parser une unique requête et de lancer la fonctionnalité demandé. La fonction renvoie un Object réponse. 
     * @param request la requête String
     * @return une réponse de type Object
     */
    private Object manageRequest(String request) {
        Object res = "OK";
        String[] parsedRequest = request.split(" ");
        // On récupère le message si des guillemets sont présentes dans la requêtes puisque le split ne va pas récupérer le message complètement
        Pattern pattern = Pattern.compile("\"(.*)\""); 
        Matcher matcher = pattern.matcher(request);
        String message = "";
        if (matcher.find()) {
            message = matcher.group(1);
        }
        String command = parsedRequest[0];
        switch (command.toLowerCase()) {
            case "set":
                if (parsedRequest.length >= 3) {
                    if (!message.isEmpty()) {
                        OpenServer.data.put(parsedRequest[1], message);
                    } else {
                        OpenServer.data.put(parsedRequest[1], parsedRequest[2]);
                    }
                    OpenServer.sendRequestToSlaves(request);
                } else {
                    res = PARAM_UNCOMPLETE;
                }
                break;
            case "setnx":
                if (parsedRequest.length >= 3) {
                    if (OpenServer.data.containsKey(parsedRequest[1])) {
                        res = 0;
                    } else {
                        res = 1;
                        if(!message.isEmpty()) {
                            OpenServer.data.put(parsedRequest[1], message);
                        } else {
                            OpenServer.data.put(parsedRequest[1], parsedRequest[2]);
                        }
                        OpenServer.sendRequestToSlaves(request.toLowerCase().replace("setnx", "set"));
                    }
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
                    res = 0;
                }
                break;
            case "append":
                if (parsedRequest.length >= 3) {
                    if(!message.isEmpty()) {
                        OpenServer.data.put(parsedRequest[1], OpenServer.data.get(parsedRequest[1])+message);
                    } else {
                        OpenServer.data.put(parsedRequest[1], OpenServer.data.get(parsedRequest[1])+parsedRequest[2]);
                    }
                    OpenServer.sendRequestToSlaves(request);
                    res = OpenServer.data.get(parsedRequest[1]).length();
                }
                break;
            case "incr":
                if (parsedRequest.length == 2 && OpenServer.data.containsKey(parsedRequest[1])) {
                    try {
                        int val = Integer.parseInt(OpenServer.data.get(parsedRequest[1]));
                        OpenServer.data.replace(parsedRequest[1], Integer.toString(val+1));
                        res = OpenServer.data.get(parsedRequest[1]);
                        OpenServer.sendRequestToSlaves(request);
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
                        res = OpenServer.data.get(parsedRequest[1]);
                        OpenServer.sendRequestToSlaves(request);
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
                res = this.deleteKeys(request, parsedRequest);
                break;
            case "expire":
                res = this.addExpiration(request, parsedRequest);
                break;
            case "subscribe":
                res = this.manageSubscription(parsedRequest);
                break;
            case "publish":
                if (parsedRequest.length >= 3) {
                    if (!message.isEmpty()) {
                        res = OpenServer.sendMessage(parsedRequest[1], parsedRequest[2]);
                    } else {
                        res = OpenServer.sendMessage(parsedRequest[1], message);
                    }
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

    /***
     * Permet à une socket abonnées à un cannal de recevoir un message depuis le serveur. 
     * Comme il n'y a pas d'écriture il n'est pas nécessaire de synchroniser cette fonction entre les différents threads.
     * @param channel le canal sur lequel le message est transmis
     * @param message le message 
     */
    public void receiveMessage(String channel, String message) {
        this.out.println(String.format("Canal : %s, Message : %s", channel, message));
    }
}
