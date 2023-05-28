package fr.ul.miage.r;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class OpenServer {
    private static final Logger logger = LoggerFactory.getLogger(OpenServer.class);

    public static Map<String, String> data = new HashMap<>();

    public static final Map<String, List<ServerHandler>> subscriptions = new HashMap<>();

    private static List<Socket> slaves;

    private static ServerSocket server;

    private static boolean master;

    private static int port = 6379;

    public static final Timer timer = new Timer();
    
    public static void main( String[] args )
    {
        try(Scanner scanner = new Scanner(System.in)) {
            String res = "";
            // On demande le type de serveur
            System.out.println("Que voulez vous faire ? \n[1] ouvrir un serveur \n[2] ouvrir un serveur esclave\n[0] quitter");
            while (!res.equals("2") && !res.equals("1") && !res.equals("0")) {
                res = scanner.nextLine();
            }
            // Si c'est un maître 
            if (res.equals("1")) {
                master = true;
                initiateServer(scanner);
                slaves = new ArrayList<>();
                logger.info("Waiting for clients");
                // l'exécution est bloqué dans la fonction suivante
                handleClientConnection(scanner);
            // si c'est un esclave 
            } else if (res.equals("2")) {
                master = false;
                initiateServer(scanner);
                logger.info("Waiting for master");
                // l'exécution est bloqué dans la fonction suivante
                handleServerConnection(scanner);
            }
        }
    }

    /**
     * Permet d'arrêter l'exécution pour un serveur maître et de connecter des slaves. 
     * On ouvre un RequestListener.
     * @param scanner
     */
    private static void handleClientConnection(Scanner scanner){
        String res = "";
        new RequestListener(server).start();
        // On bloque l'execution pour ne pas mettre fin au programme
        while (!res.equalsIgnoreCase("quit") && master) {
            res = scanner.nextLine();
            String[] parsedRes = res.split(" ");
            if (parsedRes[0].equalsIgnoreCase("connect")) {
                connectSlave(parsedRes);
            }
        }
    }

    /**
     * Permet de récupérer créer et de stocker la socket d'un esclave
     * @param parsedRes la requête qui contient les paramètres
     */
    private static void connectSlave(String[] parsedRes){
        try {
            if (Objects.nonNull(parsedRes[1])) {
                Socket slave = new Socket("127.0.0.1", Integer.parseInt(parsedRes[1]));
                slaves.add(slave);

                ObjectOutputStream out = new ObjectOutputStream(slave.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(slave.getInputStream()));
                out.writeObject(data);
                logger.info(in.readLine());

                logger.info("Slave connecté et synchronyzé");
            } else {
                logger.info("Nombre de paramètre insuffisant");
            }
        } catch (IOException | NumberFormatException e) {
            logger.warn("Impossible de se connecter au slave", e);
        }
    }

    /**
     * Permet d'arrêter l'exécution pour un serveur esclave et de le changer en serveur maître. 
     * On ouvre un RequestListener.
     * @param scanner
     */
    private static void handleServerConnection(Scanner scanner){
        try {
            Socket masterSocket;
            masterSocket = server.accept();
            new MasterListener(masterSocket).start();
            String res = "";   
            while(!res.equalsIgnoreCase("quit") && !master) {
                res = scanner.nextLine();
                if (res.equalsIgnoreCase("Make master")) {
                    master = true;
                    masterSocket.close();
                }
            }
            if (master) {
                logger.info("This server is now Master");
                handleClientConnection(scanner);
            }
            masterSocket.close();
        } catch (IOException e) {
            logger.warn("Connexion avec le master interrompue", e);
        }
    }

    /**
     * Permet de faire poursuivre une requête à tous les esclaves
     * @param request
     */
    public static void sendRequestToSlaves(String request) {
        if (Objects.nonNull(slaves) && !slaves.isEmpty()) {
            for (Socket socket : slaves) {
                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)){
                    out.println(request);
                } catch (IOException e) {
                    logger.error("Impossible d'envoyer la requête au slave", e);
                }
            }
            logger.info("synchronized");
        }
    }

    /**
     * On demande à l'utilisateur un port sur lequel ouvrir un serveur puis on ouvre le serveur sur le port demandé
     * @param scanner
     */
    private static void initiateServer(Scanner scanner) {
        while (Objects.isNull(server)) {
            try {
                System.out.println("Sur quel port ouvrir le serveur ? Default [0] : 6379");
                int tmpRes = scanner.nextInt();
                int res = 0 == tmpRes ? port : tmpRes;
                server = new ServerSocket(res);
                logger.info("Serveur ouvert sur le port {}", res);
            } catch (IOException e) {
                logger.error("Impossible d'ouvrir le serveur sur le port demandé", e);
            }
        }
    }

    /**
     * Permet d'enregistrer une socket comme étant abonnée à un canal
     * @param channel
     * @param serverHandler
     */
    public static synchronized void register(String channel, ServerHandler serverHandler) {
        String lowChannel = channel.toLowerCase();
        if (subscriptions.containsKey(lowChannel)) {
            subscriptions.get(lowChannel).add(serverHandler);
        } else {
            subscriptions.put(lowChannel, new ArrayList<>());
            subscriptions.get(lowChannel).add(serverHandler);
        }
        logger.info("Added a subscriber");
    }

    /**
     * Permet d'envoyer un message à toute les sockets connectés à un canal
     * @param channel
     * @param message
     * @return
     */
    public static synchronized String sendMessage (String channel, String message) {
        String lowChannel = channel.toLowerCase();
        if (Objects.nonNull(subscriptions.get(lowChannel)) && !subscriptions.get(lowChannel).isEmpty()) {
            for (ServerHandler subscriber : subscriptions.get(lowChannel)) {
                subscriber.receiveMessage(lowChannel, message);
            }
            return Integer.toString(subscriptions.get(lowChannel).size());
        }
        return "0";
    }

    /**
     * Permet de supprimer une socket d'un canal
     * @param channel
     * @param serverHandler
     */
    public static void unregister (String channel, ServerHandler serverHandler) {
        String lowChannel = channel.toLowerCase();
        if (subscriptions.containsKey(lowChannel) && subscriptions.get(lowChannel).contains(serverHandler)) {
            subscriptions.remove(lowChannel);
        }
    }
}
