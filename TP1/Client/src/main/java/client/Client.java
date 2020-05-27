package client;

import exceptions.MessageSizeException;
import protocol.Message;
import protocol.Request;
import protocol.Response;

import java.net.*;
import java.io.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Client {
    private ServerSocket listeningSocket;
    private int listeningPort;
    private Socket serverSocket;
    private String serverAddress;
    private int serverPort;
    private String token;
    private String user;

    public Client() {
        try {
            listeningSocket = new ServerSocket(0);
            listeningPort = listeningSocket.getLocalPort();
            listeningSocket.setReuseAddress(true);
            serverSocket = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getUser(BufferedReader reader) {
        System.out.print("Veuillez entrer le nom d'utilisateur : ");
        String username;
        try {
            username = reader.readLine();
            return username;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getPassword(BufferedReader reader) {
        System.out.print("Veuillez entrer le mot de passe : ");
        String password;
        try {
            password = reader.readLine();
            return password;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String validateIP(BufferedReader reader) {
        System.out.print("Veuillez entrez l'adresse IP du serveur : ");
        String serverAddress = "";
        boolean valid = false;
        String IP_PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        while (!valid) {
            try {
                serverAddress = reader.readLine();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            if (serverAddress.matches(IP_PATTERN)) {
                valid = true;
            } else {
                System.out.print("Adresse IP entree invalide! Veuillez entre une adresse du format XXX.XXX.XXX.XXX : ");
            }
        }
        return serverAddress;
    }

    private int validatePort(BufferedReader reader) {
        System.out.print("Veuillez entrez le port d'ecoute du serveur : ");
        int serverPort = 0;
        boolean valid = false;
        while (!valid)
        {
            try {
                serverPort = Integer.parseInt(reader.readLine());
            }
            catch(Exception e) {
                serverPort = 0;
            }
            if (serverPort >= 5000 && serverPort<=5050) {
                valid = true;
            } else {
                System.out.print("Port invalide! (Veuillez entrez un nombre entre 5000 et 5050) ");
            }
        }

        return serverPort;
    }

    private void connectToServer(String username, String password, int listeningPort, BufferedReader reader) {
        boolean connected = false;
        try {
            while (!connected) {
                serverSocket = new Socket(serverAddress, serverPort);
                DataOutputStream serverOutputStream = new DataOutputStream(serverSocket.getOutputStream());
                serverOutputStream.writeUTF(new Request("LOG_IN", "", Map.of("username", username,
                        "password", password, "listening_port", Integer.toString(listeningPort))).encodeRequest());

                DataInputStream serverInputStream = new DataInputStream(serverSocket.getInputStream());
                Response response = Response.decodeResponse(serverInputStream.readUTF());

                if (response.getResponse().equals("OK")) {
                    String token = response.getPayload().get("Token");
                    if (token != null) {
                        this.token = token;
                        connected = true;
                    }
                    serverSocket.close();
                }
                else {
                    System.out.println("Error: " + response.getPayload().get("Type"));
                    serverSocket.close();
                    serverSocket = new Socket(serverAddress, serverPort);
                    username = getUser(reader);
                    password = getPassword(reader);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printLastMessages() {
        try {
            serverSocket = new Socket(serverAddress, serverPort);
            DataOutputStream serverOutputStream = new DataOutputStream(serverSocket.getOutputStream());
            DataInputStream serverInputStream = new DataInputStream(serverSocket.getInputStream());

            Request resquest = new Request("GET_MESSAGES", token, new HashMap<>());
            serverOutputStream.writeUTF(resquest.encodeRequest());

            Response response = Response.decodeResponse(serverInputStream.readUTF());
            int size = Integer.parseInt(response.getPayload().get("size"));
            Message message;
            for (int i = 1; i <= size; ++i) {
                message = Message.decodeMessage(response.getPayload().get(Integer.toString(size-i)));
                if (message != null) {
                    System.out.println(message.toConsole());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        serverAddress = validateIP(reader);
        serverPort = validatePort(reader);
        user = getUser(reader);
        String password = getPassword(reader);

        ReadMessage readMessage = new ReadMessage();
        readMessage.start();

        connectToServer(user, password, listeningSocket.getLocalPort(), reader);
        printLastMessages();

        SendMessage sendMessage = new SendMessage();
        sendMessage.start();
    }


    private class SendMessage extends Thread {
        BufferedReader userIn;

        public SendMessage() {
            userIn = new BufferedReader(new InputStreamReader(System.in));
        }

        public void run() {
            String inputMessage;
            Message message;
            Request request;

            while (true) {
                try {
                    inputMessage = userIn.readLine();
                    serverSocket = new Socket(serverAddress, serverPort);
                    DataOutputStream outputStream = new DataOutputStream(serverSocket.getOutputStream());
                    DataInputStream inputStream = new DataInputStream(serverSocket.getInputStream());

                    try {
                        message = new Message(user, serverSocket.getInetAddress().toString(), serverSocket.getLocalPort(),
                                Instant.now(), inputMessage);
                        request = new Request("NEW_MESSAGE", token, Map.of("Message", message.encodeMessage()));
                        outputStream.writeUTF(request.encodeRequest());
                        Response response = Response.decodeResponse(inputStream.readUTF());
                    } catch (MessageSizeException e) {
                        System.out.println("Erreur: la taille du message doit être de 200 caractères ou moins.");
                    } finally {
                        serverSocket.close();
                    }
                }
                catch(IOException e) {
                    System.out.println("Erreur dans l'envoi du message! Déconnexion.");
                }
            }
        }
    }

    private class ReadMessage extends Thread {
        private boolean running;

        public ReadMessage() {
            running = true;
        }

        public void run() {
            while (running) {
                try {
                    Socket currentServerSocket = listeningSocket.accept();
                    DataInputStream inputStream = new DataInputStream(currentServerSocket.getInputStream());
                    Message message = Message.decodeMessage(inputStream.readUTF());
                    if (message != null) {
                        System.out.println(message.toConsole());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try{
                        listeningSocket.close();
                        listeningSocket = new ServerSocket(listeningPort);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void terminate() {
            running = false;
        }
    }


    public static void main(String[] args) throws Exception
    {
        Client client = new Client();
        client.run();
    }
}
