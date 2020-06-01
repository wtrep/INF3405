package client;

import exceptions.MessageSizeException;
import protocol.Message;
import protocol.Request;
import protocol.Response;

import java.io.*;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Client {
    private String token;
    private String username;

    public Client() {
        token = "";
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    private String validateIP(BufferedReader reader) {
        System.out.print("Veuillez entrez l'adresse IP du serveur : ");
        String serverAddress = "";
        boolean valid = false;
        String IP_PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        while (!valid) {
            try {
                serverAddress = reader.readLine();
            } catch (IOException e) {
            } finally {
                if (serverAddress.matches(IP_PATTERN)) {
                    valid = true;
                } else {
                    System.out.print("Adresse IP entree invalide! Veuillez entre une adresse du format XXX.XXX.XXX.XXX : ");
                }
            }
        }
        return serverAddress;
    }

    private int validatePort(BufferedReader reader) {
        System.out.print("Veuillez entrez le port d'ecoute du serveur : ");
        int serverPort = 0;
        boolean valid = false;
        while (!valid) {
            try {
                serverPort = Integer.parseInt(reader.readLine());
            } catch (NumberFormatException e) {
                serverPort = 0;
            } catch (IOException e) {
            } finally {
                if (serverPort >= 5000 && serverPort <= 5050) {
                    valid = true;
                } else {
                    System.out.print("Port invalide! (Veuillez entrez un nombre entre 5000 et 5050) ");
                }
            }
        }

        return serverPort;
    }

    private Map<String, String> sendRequest(String serverAddress, int serverPort, String requestID, Map<String, String> payload) {
        try {
            Socket socket = new Socket(serverAddress, serverPort);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            Request request = new Request(
                    requestID,
                    token,
                    payload
            );
            outputStream.writeUTF(request.encodeRequest());
            Response response = Response.decodeResponse(inputStream.readUTF());
            socket.close();
            if (response.getResponse().equals("OK")) {
                return response.getPayload();
            } else {
                if (response.getPayload().get("Type").matches("Wrong password")) {
                    System.out.println("Erreur dans la saisie du mot de passe");
                } else {
                    System.out.println("Error: " + response.getPayload().get("Type"));
                }
            }
        } catch (ConnectException e) {
            System.out.println("Erreur: Incapable de joindre le serveur");
        } catch (IOException e) {
        }
        return new HashMap<>();
    }

    private void login(BufferedReader reader, String serverAddress, int serverPort, int listeningPort) {
        try {
            System.out.print("Veuillez entrer le nom d'utilisateur : ");
            username = reader.readLine();
            System.out.print("Veuillez entrer le mot de passe : ");
            String password = reader.readLine();
            Map<String, String> requestPayload = Map.of(
                    "listening_port", Integer.toString(listeningPort),
                    "username", username,
                    "password", password
            );
            Map<String, String> responsePayload = sendRequest(serverAddress, serverPort, "LOG_IN", requestPayload);
            token = responsePayload.get("Token") != null ? responsePayload.get("Token") : "";
        } catch (IOException e) {
        }
    }

    private void logout(BufferedReader reader, String serverAddress, int serverPort) {
        Map<String, String> requestPayload = Map.of();
        Map<String, String> responsePayload = sendRequest(serverAddress, serverPort, "LOG_OUT", requestPayload);
        String username = responsePayload.get("username");
        if (username != null) {
            token = "";
            System.out.println("Logout avec succès: " + username);
        }
    }

    private void printLastMessages(BufferedReader reader, String serverAddress, int serverPort) {
        Map<String, String> requestPayload = Map.of();
        Map<String, String> responsePayload = sendRequest(serverAddress, serverPort, "GET_MESSAGES", requestPayload);
        int size = Integer.parseInt(responsePayload.get("size"));
        Message message;
        for (int i = 1; i <= size; ++i) {
            message = Message.decodeMessage(responsePayload.get(Integer.toString(size - i)));
            if (message != null) {
                System.out.println(message.toConsole());
            }
        }
    }

    private boolean selectAction(BufferedReader reader, String serverAddress, int serverPort) {
        if (token.matches("")) {
            try {
                ServerSocket listeningSocket = new ServerSocket(0);
                ReadMessage messageListener = new ReadMessage(listeningSocket);
                messageListener.setDaemon(true);
                messageListener.start();
                login(reader, serverAddress, serverPort, listeningSocket.getLocalPort());
                if (token.matches("")) {
                    return false;
                }
                printLastMessages(reader, serverAddress, serverPort);
                System.out.println("Bienvenue au serveur chat d'INF3405");
                System.out.println("Pour se déconnecter, veuillez entrer /logout");
                System.out.println("Pour quitter l'application, veuillez entrer /exit");
            } catch (IOException e) {
            }
        } else {
            try {
                String action = reader.readLine();
                if (action.equals("/logout")) {
                    logout(reader, serverAddress, serverPort);
                    return false;
                } else if (action.equals("/exit")) {
                    logout(reader, serverAddress, serverPort);
                    return true;
                } else {
                    new SendMessage(serverAddress, serverPort, action).start();
                }
            } catch (IOException e) {
            }
        }
        return false;
    }

    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String serverAddress = validateIP(reader);
        int serverPort = validatePort(reader);
        boolean quit = false;
        while (!quit) {
            quit = selectAction(reader, serverAddress, serverPort);
        }
    }

    private class SendMessage extends Thread {
        String serverAddress;
        int serverPort;
        String inputMessage;

        public SendMessage(String serverAddress, int serverPort, String inputMessage) {
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.inputMessage = inputMessage;
        }

        public void run() {
            try {
                Message message = new Message(username, serverAddress, serverPort, inputMessage);
                Map<String, String> requestPayload = Map.of("Message", message.encodeMessage());
                sendRequest(serverAddress, serverPort, "NEW_MESSAGE", requestPayload);
            } catch (MessageSizeException e) {
                System.out.println("Erreur: la taille du message doit être de 200 caractères ou moins.");
            }
        }
    }

    private class ReadMessage extends Thread {
        private boolean running;
        ServerSocket listeningSocket;

        public ReadMessage(ServerSocket listeningSocket) {
            this.listeningSocket = listeningSocket;
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
                    listeningSocket.close();
                    listeningSocket = new ServerSocket(listeningSocket.getLocalPort());
                } catch (Exception e) {
                }
            }
        }

        public void terminate() {
            running = false;
        }
    }
}
