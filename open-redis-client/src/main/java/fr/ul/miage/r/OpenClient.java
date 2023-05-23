package fr.ul.miage.r;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class OpenClient 
{
    public static final Logger logger = LoggerFactory.getLogger(OpenClient.class);

    public static void main( String[] args )
    {
        String request = "";
        Socket socket = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        System.out.println("Vous pouvez commencer à taper des commandes !");
        try(Scanner scanner = new Scanner(System.in)) {
            while(!"exit".equalsIgnoreCase(request)) {
                request = scanner.nextLine();
                try {
                    //establish socket connection to server
                    socket = new Socket("localhost", 6379);

                    //write to socket using ObjectOutputStream
                    oos = new ObjectOutputStream(socket.getOutputStream());
                    logger.info("Sending request to Socket Server");
                    oos.writeObject(request);

                    //read the server response message
                    ois = new ObjectInputStream(socket.getInputStream());
                    String message = (String) ois.readObject();
                    System.out.println("Réponse: " + message);
                    
                    //close resources
                    ois.close();
                    oos.close();
                    socket.close();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    logger.error("Erreur lors de l'envoie ou de la récupération de la requête", e);
                } 
            } 
        }      
    }
}
