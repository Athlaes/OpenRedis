package fr.ul.miage.r;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
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

    private static ServerSocket server;

    private static int port = 6379;

    public static final Timer timer = new Timer();
    
    public static void main( String[] args )
    {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            logger.error("Impossible d'ouvrir le serveur sur le port 6379", e);
        }
        logger.info("Waiting for clients");
        try {
            while (true) {
                new ServerHandler(server.accept()).start();
            }
        } catch (IOException e) {
            logger.error("Impossible d'ouvrir le serveur sur le port 6379", e);
        }
    }
}
