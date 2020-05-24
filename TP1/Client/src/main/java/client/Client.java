package client;

import java.net.*;
import java.io.*;

public class Client {
    private static Socket socket;

    /*
     * Application client
     */

    private static String validationIP(BufferedReader reader) {
        //Demande IP
        System.out.println("Veuillez entrez l'adresse IP du serveur : ");
        //Initialisation de variable
        String serverAddress = "";
        String IP_PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
        while (!serverAddress.matches(IP_PATTERN)) {
            //Donnees entree par la console
            try {
                serverAddress = reader.readLine();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            if(!serverAddress.matches(IP_PATTERN))
                System.out.println("Adresse IP entrée invalide! Veuillez entre une adresse du format XXX.XXX.XXX.XXX : ");
        }
        
        return serverAddress;
    }

    private static int validationPort(BufferedReader reader) {
        //Demande Port
        System.out.println("Veuillez entrez le port d'écoute du serveur : ");
        int serverPort = 0;
        while( serverPort<5000 || serverPort>5050 )
        {
            //Donnees entrées par la console
            try {
                serverPort = Integer.parseInt(reader.readLine());
            }
            catch(Exception e) {
                serverPort = 0;
            }
            if (!(serverPort<5000 || serverPort>5050))
                System.out.println("Port invalide! (Veuillez entrez un nombre entre 5000 et 5050) ");
        }

        return serverPort;
    }

    private static class SendMessage extends Thread {
        BufferedReader userIn; 
        BufferedWriter writer;  
        boolean connected;

        public SendMessage(Socket socket) {
            userIn = new BufferedReader(new InputStreamReader(System.in));
            connected = true;
            try {
                writer = new BufferedWriter(new PrintWriter(socket.getOutputStream()));
            } catch (IOException e) {
                System.out.println("Le socket n'est pas connecté veuillez réessayer.");
                connected = false;
            }
        }

        public void run() {
            String message;
            String inputMessage;
            while(true) {
                try {
                    //Chercher input
                    inputMessage = userIn.readLine();

                    //TODO Former message
                    message = inputMessage;

                    //Envoyer le message
                    writer.write(message);
                    writer.newLine();
                    writer.flush();
                }
                catch(IOException e) {
                    System.out.println("Erreur dans l'envoi du message! Déconnexion.");
                    connected = false;
                }
            }
        }
    }

    private static class ReadMessage extends Thread {
        BufferedReader receiver;
        boolean connected;

        public ReadMessage(Socket socket) {
            connected = true;
            try {
                receiver = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                System.out.println("Le socket n'est pas connecté veuillez réessayer.");
                connected = false;
            }
        }

        public void run() {
            String message;
            while(connected) {
                try {
                    //Chercher le message
                    message = receiver.readLine();

                    //Afficher le message
                    System.out.println(message);
                }
                catch(IOException e) {
                    System.out.println("Erreur dans la réception du message! Déconnexion.");
                    connected = false;
                }
            }
        }
    }


    public static void main(String[] args) throws Exception
    {
        //Issue #11 : Saisie des paramètres du serveur (adresse IP, port d'écoute entre 5000 et 5050)
        //Canal d'entree de la console
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        //Addresse et port du serveur
        String serverAddress = validationIP(reader);
        int port= validationPort(reader);

        //TEST pour receiveMessage(socket)
        //Creation d'une nouvelle connexion avec le serveur
        socket = new Socket(serverAddress, port);
//		BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        try {
            //Écriture, reception
            SendMessage send = new SendMessage(socket);
            send.start();
            ReadMessage read = new ReadMessage(socket);
            read.start();
            //Attendre que les threads finissent
            send.join();
            read.join();
        }
        finally {
            System.out.println("Tu as été déconnecté du serveur!");

            //Fermeture de la connexion avec le serveur
            socket.close();
        }
    }
}
