package fr.ul.miage.r;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterListener extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(MasterListener.class);

    private Socket masterSocket;

    public MasterListener(Socket socket) {
        this.masterSocket = socket;
    }

    @Override
    public void run(){
        // On ouvre un ObjectInputStream pour lire les objets qui proviennent de la synchronisation 
        // Le reste des requêtes est lues avec le BufferedReader
        try (ObjectInputStream inObject = new ObjectInputStream(masterSocket.getInputStream());
            BufferedReader in = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()));
            PrintWriter out = new PrintWriter(masterSocket.getOutputStream(), true)) {
            String line;
            // On synchronise les données 
            this.synchronizeData(inObject, out);
            // On écoute les prochaines requêtes du master
            while ((line = in.readLine()) != null) {
                this.manageRequest(line);
                logger.info("Got data");
            }
        } catch (IOException e) {
            logger.error("Impossible de contacter le master", e);
        } catch (ClassNotFoundException e) {
            logger.error("Impossible de parser l'objet", e);
        }
    }

    /***
     * Cette méthode permet de récupérer la map data du serveur master se connectant au slave.
     * Pour lire cette map on ouvre un ObjectInputStream qu'on ne ferme pas pour ne pas fermer l'inputStream.
     * @param inObject lis l'objet passant dans l'inputstream
     * @param out printwriter
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void synchronizeData(ObjectInputStream inObject, PrintWriter out) throws ClassNotFoundException, IOException {
        OpenServer.data = (Map<String, String>) inObject.readObject();
        out.println("Slave OK");
        logger.info("Data synchronized.");
    }

    /**
     * Permet de parser une unique requête et de lancer la fonctionnalité demandé. La fonction renvoie un Object réponse. 
     * Dans ce contexte on effectue moins de vérification car on est sûr que les réponses qui proviennent du master sont correctes.
     * @param request la requête String
     * @return une réponse de type Object
     */
    private void manageRequest(String request) {
        String[] parsedRequest = request.split(" ");
        // On récupère le message si des guillemets sont présentes dans la requêtes puisque le split ne va pas récupérer le message complètement
        Pattern pattern = Pattern.compile("\"(.*)\""); 
        Matcher matcher = pattern.matcher(request);
        String message = "";
        if (matcher.find()) {
            message = matcher.group(1);
        }
        String command = parsedRequest[0];
        int val;
        switch (command.toLowerCase()) {
            case "set":
                if (!message.isEmpty()) {
                    OpenServer.data.put(parsedRequest[1], message);
                } else {
                    OpenServer.data.put(parsedRequest[1], parsedRequest[2]);
                }
                break;
            case "append":
                if(!message.isEmpty()) {
                    OpenServer.data.put(parsedRequest[1], OpenServer.data.get(parsedRequest[1])+message);
                } else {
                    OpenServer.data.put(parsedRequest[1], OpenServer.data.get(parsedRequest[1])+parsedRequest[2]);
                }
                break;
            case "incr":
                val = Integer.parseInt(OpenServer.data.get(parsedRequest[1]));
                OpenServer.data.replace(parsedRequest[1], Integer.toString(val+1));
                break;
            case "decr":
                val = Integer.parseInt(OpenServer.data.get(parsedRequest[1]));
                OpenServer.data.replace(parsedRequest[1], Integer.toString(val-1));
                break;
            case "del":
                this.deleteKeys(parsedRequest);
                break;
            case "expire":
                this.addExpiration(parsedRequest);
                break;
            default:
                break;
        }
    }

    private void deleteKeys(String[] parsedRequest){
        for(int i = 1; i < parsedRequest.length; i++){
            if (OpenServer.data.containsKey(parsedRequest[i])) {
                OpenServer.data.remove(parsedRequest[i]);
            }
        }
    }

    private void addExpiration(String[] parsedRequest) {
        final String key = parsedRequest[1];
        try {
            OpenServer.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    OpenServer.data.remove(key);
                }
            }, Long.parseLong(parsedRequest[2]));
        } catch (NumberFormatException e) {
            logger.error("bad value", e);
        }
    }
}
