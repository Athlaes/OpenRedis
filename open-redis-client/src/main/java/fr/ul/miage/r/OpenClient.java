package fr.ul.miage.r;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class OpenClient {
    public static final Logger logger = LoggerFactory.getLogger(OpenClient.class);

    public static boolean subscription = false;

    public static void main( String[] args )
    {
        String request = "";
        PrintWriter out = null;
        BufferedReader in = null;
        try(Scanner scanner = new Scanner(System.in)) {
            logger.info("Sur quel port voulez vous lancé vos requêtes ? (serveur local)");
            int port = scanner.nextInt();
            scanner.nextLine();
            logger.info("Vous pouvez commencer à taper des commandes !");
            while(!"quit".equalsIgnoreCase(request)) {
                request = scanner.nextLine();
                try (Socket socket = new Socket("localhost", port);) {
                    //write to socket using ObjectOutputStream
                    out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(request);
                    
                    String req = request.split(" ")[0];
                    subscription = true;
                    if (req.equalsIgnoreCase("subscribe")) {
                        new Listener(socket).start();
                        while (subscription && !"unsubscribe".equals(request)) {
                            request = scanner.nextLine();
                            out.println(request);
                        }
                    }
                    //read the server response message
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line = "";
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                    
                    //close resources
                    in.close();
                    out.close();
                } catch (IOException e) {
                    logger.error("Erreur lors de l'envoie ou de la récupération de la requête", e);
                }
            } 
        }      
    }
}
