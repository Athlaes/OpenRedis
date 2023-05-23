package fr.ul.miage.r;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Listener extends Thread {
    private Socket socket;

    public Listener(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run(){
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if (line.contains("paramètre insuffisant")) {
                    OpenClient.subscription = false;
                }
            }
        } catch (IOException e) {
            OpenClient.logger.warn("Le listener s'est interrompu", e);
        }
    }
}
