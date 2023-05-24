package fr.ul.miage.r;

import java.io.IOException;
import java.net.ServerSocket;
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
public class OpenServer
{
    private static final Logger logger = LoggerFactory.getLogger(OpenServer.class);

    public static final Map<String, String> data = new HashMap<>();

    public static final Map<String, List<ServerHandler>> subscriptions = new HashMap<>();

    private static ServerSocket server;

    private static int port = 6379;

    public static final Timer timer = new Timer();

    private static boolean slave = false;
    
    public static void main( String[] args )
    {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            logger.error("Impossible d'ouvrir le serveur sur le port 6379", e);
        }
        logger.info("Waiting for clients");
        new RequestListener(server).start();
        try(Scanner scanner = new Scanner(System.in)) {
            String req = "";
            while (!req.equalsIgnoreCase("exit") && !slave) {
                req = scanner.nextLine();
            }
        }
    }

    public static void register(String channel, ServerHandler serverHandler) {
        String lowChannel = channel.toLowerCase();
        if (subscriptions.containsKey(lowChannel)) {
            subscriptions.get(lowChannel).add(serverHandler);
        } else {
            subscriptions.put(lowChannel, new ArrayList<>());
            subscriptions.get(lowChannel).add(serverHandler);
        }
        logger.info("Added a subscriber");
    }

    public static String sendMessage (String channel, String message) {
        String lowChannel = channel.toLowerCase();
        if (Objects.nonNull(subscriptions.get(lowChannel)) && !subscriptions.get(lowChannel).isEmpty()) {
            for (ServerHandler subscriber : subscriptions.get(lowChannel)) {
                subscriber.receiveMessage(lowChannel, message);
            }
            return Integer.toString(subscriptions.get(lowChannel).size());
        }
        return "0";
    }

    public static void unregister (String channel, ServerHandler serverHandler) {
        String lowChannel = channel.toLowerCase();
        if (subscriptions.containsKey(lowChannel) && subscriptions.get(lowChannel).contains(serverHandler)) {
            subscriptions.remove(lowChannel);
        }
    }
}
