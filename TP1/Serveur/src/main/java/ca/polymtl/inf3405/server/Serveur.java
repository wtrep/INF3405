package ca.polymtl.inf3405.server;

import ca.polymtl.inf3405.database.Database;
import ca.polymtl.inf3405.exceptions.DatabaseInsertionException;
import ca.polymtl.inf3405.exceptions.NoUserException;
import ca.polymtl.inf3405.protocol.Message;
import ca.polymtl.inf3405.protocol.Request;
import ca.polymtl.inf3405.protocol.Response;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Serveur
{
	private volatile static ConcurrentMap<String, ConnectedUser> connectedUsers;
    private volatile static BlockingQueue<Message> messagesQueue;
    private static ServerSocket listener;

    public Serveur() {
    	connectedUsers = new ConcurrentHashMap<>();
    	messagesQueue = new LinkedBlockingQueue<>();
	}

    private void initiateServer() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String serverAddress = validateIP(reader);
		int serverPort= validatePort(reader);

		try {
			listener = new ServerSocket();
			listener.setReuseAddress(true);
			InetAddress serverIP = InetAddress.getByName(serverAddress);
			listener.bind(new InetSocketAddress(serverIP, serverPort));
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);
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

	private void run() {
		initiateServer();
		MessageHandler messageHandler = new MessageHandler(connectedUsers, messagesQueue);
		messageHandler.start();

		try
		{
			while(true)
			{
				ClientHandler client = new ClientHandler(listener.accept(), connectedUsers, messagesQueue);
				client.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally
		{
			try {
				listener.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class ClientHandler extends Thread
	{
		private Socket socket;
		private DataInputStream reader;
		private DataOutputStream writer;
		private Database database;
		private volatile ConcurrentMap<String, ConnectedUser> connectedUsers;
		private volatile BlockingQueue<Message> messagesQueue;
		
		public ClientHandler(Socket socket, ConcurrentMap<String, ConnectedUser> connectedUsers,
							 BlockingQueue<Message> messagesQueue)
		{
			this.socket = socket;
			try {
				this.reader = new DataInputStream(socket.getInputStream());
				this.writer = new DataOutputStream(socket.getOutputStream());
				this.database = Database.getInstance();
				this.connectedUsers = connectedUsers;
				this.messagesQueue = messagesQueue;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run()
		{
			try {
				Request request = Request.decodeRequest(reader.readUTF());
				processRequest(request);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void processRequest(Request request) {
			switch (request.getRequest()) {
				case "LOG_IN":
					processLogIn(request);
					break;
				case "LOG_OUT":
					processLogOut(request);
					break;
				case "NEW_MESSAGE":
					processNewMessage(request);
					break;
				case "GET_MESSAGES":
					processMessagesRequest(request);
					break;
				default:
					sendErrorResponse(Map.of("Type", "Wrong request"));
			}
		}

		private void processLogIn(Request request) {
			String username = request.getPayload().get("username");
			String password = request.getPayload().get("password");
			String port = request.getPayload().get("listening_port");
			if (username == null || password == null) {
				sendErrorResponse(Map.of("Type", "No username"));
				return;
			}
			int listeningClientPort;
			try {
				listeningClientPort = Integer.parseInt(port);
			} catch (NumberFormatException e) {
				sendErrorResponse(Map.of("Type", "Wrong port format"));
				return;
			}
			if (listeningClientPort < 1025 || listeningClientPort > 49152) {
				sendErrorResponse(Map.of("Type", "Wrong port number"));
				return;
			}

			try {
				User user = database.getUser(username);
				if (user.isGoodPassword(password)) {
					ConnectedUser connectedUser = new ConnectedUser(user, socket.getInetAddress(), listeningClientPort);
					connectedUsers.put(connectedUser.getToken(), connectedUser);
					Response response = new Response("OK", Map.of("Token", connectedUser.getToken()));
					sendResponse(response);
				}
				else {
					sendErrorResponse(Map.of("Type", "Wrong password"));
				}
			} catch (NoUserException e) {
				processNewUser(request, username, password, listeningClientPort);
			}
		}

		private void processNewUser(Request request, String username, String password, int port) {
			User user = new User(username);
			user.setPassword(password);
			try {
				database.insertNewUser(user);
			} catch (DatabaseInsertionException e) {
				sendErrorResponse(Map.of("Type", "Database insertion error"));
			}
			ConnectedUser connectedUser = new ConnectedUser(user, socket.getInetAddress(), port);
			connectedUsers.put(connectedUser.getToken(), connectedUser);
			Response response = new Response("OK", Map.of("NewUser", "true", "Token",
					connectedUser.getToken()));
			sendResponse(response);
		}

		private void processLogOut(Request request) {
			String token = request.getToken();
			ConnectedUser connectedUser = connectedUsers.remove(token);
			if (connectedUser == null) {
				sendErrorResponse(Map.of("Type", "Wrong token"));
			} else {
				Response response = new Response("OK", Map.of("username", connectedUser.getUserName()));
				sendResponse(response);
			}
		}

		private void processNewMessage(Request request) {
			ConnectedUser connectedUser = connectedUsers.get(request.getToken());
			if (connectedUser == null) {
				sendErrorResponse(Map.of("Type", "Wrong token"));
			} else {
				String encodedMessage = request.getPayload().get("Message");
				if (encodedMessage == null) {
					sendErrorResponse(Map.of("Type", "No message"));
					return;
				}
				Message message = Message.decodeMessage(encodedMessage);
				if (message == null) {
					sendErrorResponse(Map.of("Type", "Wrong message formatting"));
					return;
				}
				messagesQueue.add(message);
				Response response = new Response("OK", Map.of("username", connectedUser.getUserName()));
				sendResponse(response);
			}
		}

		private void processMessagesRequest(Request request) {
			ConnectedUser connectedUser = connectedUsers.get(request.getToken());
			if (connectedUser == null) {
				sendErrorResponse(Map.of("Type", "Wrong token"));
			} else {
				int NUMBER_OF_MESSAGES = 15;
				List<Message> messages = database.getLastMessages(NUMBER_OF_MESSAGES);
				HashMap<String, String> payload = new HashMap<>();
				int i = 0;

				payload.put("size", Integer.toString(messages.size()));
				for (Message m : messages) {
					payload.put(Integer.toString(i++), m.encodeMessage());
				}
				Response response = new Response("OK", payload);
				sendResponse(response);
			}
		}

		private void sendErrorResponse(Map<String, String> payload) {
			Response response = new Response("ERROR", payload);
			sendResponse(response);
		}

		private void sendResponse(Response response) {
			try {
				writer.writeUTF(response.encodeResponse());
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class MessageHandler extends Thread {
		private volatile ConcurrentMap<String, ConnectedUser> connectedUsers;
		private volatile BlockingQueue<Message> messagesQueue;
		private volatile Boolean running = true;
		private Database database;
		public MessageHandler(ConcurrentMap<String, ConnectedUser> connectedUsers,
							  BlockingQueue<Message> messagesQueue) {
			this.connectedUsers = connectedUsers;
			this.messagesQueue = messagesQueue;
			database = Database.getInstance();
		}

		public void run() {
			Message message;
			while (running) {
				try {
					message = messagesQueue.take();
					sendMessage(message);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		private void sendMessage(Message message) {
			Map<String, ConnectedUser> users = Collections.unmodifiableMap(connectedUsers);
			users.forEach((k,u) -> {
				MessageSender sender = new MessageSender(u, message);
				sender.start();
			});
			try {
				database.insertNewMessage(message);
			} catch (DatabaseInsertionException e) {
				e.printStackTrace();
			}
			System.out.println(message.getMessage());
		}

		public void terminate() {
			running = false;
		}
	}

	private class MessageSender extends Thread {
    	private final ConnectedUser user;
    	private final Message message;

    	MessageSender(ConnectedUser user, Message message) {
    		this.user = user;
    		this.message = message;
		}

    	public void run() {
			try {
				Socket socket = new Socket(user.getUserAddress(), user.getUserPort());
				DataOutputStream output = new DataOutputStream(socket.getOutputStream());
				output.writeUTF(message.encodeMessage());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws Exception
	{
		Serveur serveur = new Serveur();
		serveur.run();
	}
}
