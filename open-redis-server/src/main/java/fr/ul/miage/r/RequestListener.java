package fr.ul.miage.r;

import java.io.IOException;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestListener extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(RequestListener.class);

    private ServerSocket server;

    public RequestListener(ServerSocket server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            while (true) {
                new ServerHandler(server.accept()).start();
            }
        } catch (IOException e) {
            logger.error("Impossible d'ouvrir le serveur sur le port 6379", e);
        }
    }
}
