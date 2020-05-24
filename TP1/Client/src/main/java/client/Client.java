package client;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStreamReader;

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
        boolean valid = false;
        int octet1, octet2, octet3, octet4;

        while (!valid) {
            //Donnees entrée par la console
            try {
                serverAddress = reader.readLine();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            //Vérification de la validité des données
            int nbrDePoints = serverAddress.length()-serverAddress.replace(".", "").length();
            if (serverAddress.contains(".") && nbrDePoints==3) {
                String[] octets = serverAddress.split("\\."); // Les backslash sont utilises pour echapper valeur special de .
                if(octets.length == 4) {
                    try {
                        octet1 = Integer.parseInt(octets[0]);
                        octet2 = Integer.parseInt(octets[1]);
                        octet3 = Integer.parseInt(octets[2]);
                        octet4 = Integer.parseInt(octets[3]);
                    }
                    catch (NumberFormatException e)
                    {
                        octet1 = octet2 = octet3 = octet4 = 128;
                    }

                    if(octet1 >= 0 && octet1 < 128 ) {
                        if(octet2 >= 0 && octet2 < 128 ) {
                            if(octet3 >= 0 && octet3 < 128 ) {
                                if(octet4 >= 0 && octet4 < 128 ) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
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
            if (serverPort>=5000 && serverPort<=5050) //Pour ne pas afficher message d'erreur si correct
                break;
            //Message d'erreur
            System.out.println("Port invalide! (Veuillez entrez un nombre entre 5000 et 5050) ");
        }

        return serverPort;
    }

    private static class SendMessage extends Thread {
        BufferedReader input;
        BufferedWriter output;
        boolean connected;

        public SendMessage(Socket socket) {
            input = new BufferedReader(new InputStreamReader(System.in));
            connected = true;
            try {
                output = new BufferedWriter(new PrintWriter(socket.getOutputStream()));
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
                    inputMessage = input.readLine();

                    //TODO Former message
                    message = inputMessage;

                    //Envoyer le message
                    output.write(message);
                    output.newLine();
                    output.flush();
                }
                catch(IOException e) {
                    System.out.println("Erreur dans l'envoi du message! Déconnexion.");
                    connected = false;
                }
            }
        }
    }

    private static class ReadMessage extends Thread {
        BufferedReader input;
        boolean connected;

        public ReadMessage(Socket socket) {
            connected = true;
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
                    message = input.readLine();

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
//		int port = 5000;
//		String serverAddress = "127.0.0.1";

        //TEST pour receiveMessage(socket)
        //Creation d'une nouvelle connexion avec le serveur
        socket = new Socket(serverAddress, port);
//		BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        try {
            //Écriture, reception
            if (true) {
                new SendMessage(socket).start();
                new ReadMessage(socket).start();
            }
            while(true) {}
        }
        finally {
            System.out.println("Tu as été déconnecté du serveur!");

            //Fermeture de la connexion avec le serveur
            socket.close();
        }
    }
}

