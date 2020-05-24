package ca.polymtl.inf3405.server;

import ca.polymtl.inf3405.protocol.Message;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Serveur {
    private volatile static Map<String, User> connectedClients;
    private volatile static Queue<Message> messagesQueue;

    private static ServerSocket listener;
    public static List<ClientHandler> clients;

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

    public static void main(String[] args) throws Exception
    {
        //Canal d'entree de la console
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        //Validation adresse IP
        String serverAddress = validationIP(reader);
        //Validation port
        int serverPort= validationPort(reader);
//		String serverAddress = "127.0.0.1";
//		int serverPort = 5000;

        String username ="username";

        listener = new ServerSocket();
        listener.setReuseAddress(true);
        InetAddress serverIP = InetAddress.getByName(serverAddress);

        //Association de l'adresse et du port a la connexion
        listener.bind(new InetSocketAddress(serverIP, serverPort));

        System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);


        try
        {
            /*
             * A chaque fois qu'un nouveau client se connecte, on execute la fonction
             * Run() de l'objet ClientHandler
             */
            clients = new ArrayList<>();
            while(true)
            {
                //Important: la fonction accept() est bloquante : attend qu'un prochain client se connecte
                // Une nouvelle connexion
                ClientHandler client = new ClientHandler(listener.accept(), username);
                clients.add(client);
                System.out.println("Client added");
                client.start();
            }
        }
        finally
        {
            //Fermeture du socket d'écoute
            listener.close();
        }
    }


    /*
     * Un thread qui se charge de traiter la demande de chaque client
     * sur un socket particulier
     */
    private static class ClientHandler extends Thread
    {
        private Socket socket;
        private String username;
        private BufferedReader reader;
        private BufferedWriter writer;
        boolean isLoggedIn;

        public ClientHandler(Socket socket,String username)
        {
            this.socket = socket;
            this.username = username;
            System.out.println("New connection with " + username + " at " + socket);
            isLoggedIn=true;
            try {
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.writer = new BufferedWriter(new PrintWriter(socket.getOutputStream()));
            } catch (IOException e) {
                System.out.println("Erreur lors de la connexion, veuillez réessayer!");
                isLoggedIn=false;
            }
        }

        /*
         * Un thread qui se charge d'un client.
         */
        public void run()
        {
            String received;
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd k:m:s");
            while(isLoggedIn) {
                try {
                    //Recevoir messages
                    received = reader.readLine();

                    //TODO Transforme received into message
                    String message = "["+username+" - "+"IPaddress:Port"+" - "+LocalDateTime.now().format(timeFormat)+"]:"+received;

                    //Afficher sur console serveur
                    System.out.println(message);

                    //Pour tous les clients, TODO :ajouter filtre par après
                    for(ClientHandler client : Server.clients) {
                        client.getOutput().write(message);
                        client.getOutput().newLine();
                        client.getOutput().flush();
                    }

                    if(received == "logout") {
                        isLoggedIn=false;
                    }



                } catch (Exception e) {
                    System.out.println("La connexion a été coupée avec " + this.username + "!");
                    isLoggedIn=false;
                    clients.remove(this);
                }
            }
            try {
                //Relâcher les ressources
                this.reader.close();
                this.writer.close();
                this.socket.close();
            } catch (IOException e ){
                System.out.println("Erreur dans la fermeture des buffers!");
            }
        }

        private BufferedWriter getOutput() {
            return writer;
        }
    }

}
