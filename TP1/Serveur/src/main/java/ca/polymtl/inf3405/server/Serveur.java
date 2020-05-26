package ca.polymtl.inf3405.server;

import ca.polymtl.inf3405.protocol.Message;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Serveur
{
	private volatile static Map<String, User> connectedClients;
    private volatile static Queue<Message> messagesQueue;
    private static ServerSocket listener;
    public static List<ClientHandler> clients;

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
		System.out.println("Veuillez entrez l'adresse IP du serveur : ");
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
				break;
			}
			System.out.println("Adresse IP entree invalide! Veuillez entre une adresse du format XXX.XXX.XXX.XXX : ");
		}
		return serverAddress;
	}

	private int validatePort(BufferedReader reader) {
		System.out.println("Veuillez entrez le port d'ecoute du serveur : ");
		int serverPort = 0;
		while (serverPort<5000 || serverPort>5050)
		{
			try {
				serverPort = Integer.parseInt(reader.readLine());
			}
			catch(Exception e) {
				serverPort = 0;
			}
			if (serverPort>=5000 && serverPort<=5050)
				break;
			System.out.println("Port invalide! (Veuillez entrez un nombre entre 5000 et 5050) ");
		}
		
		return serverPort;
	}

	private void run() {
		initiateServer();

		try
		{
			while(true)
			{
				ClientHandler client = new ClientHandler(listener.accept());
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
		
		public ClientHandler(Socket socket)
		{
			this.socket = socket;
			try {
				this.reader = new DataInputStream(socket.getInputStream());
				this.writer = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run()
		{
			String request = reader.read
		}
	}

	private class MessageHandler extends Thread

	public static void main(String[] args) throws Exception
	{
		Serveur serveur = new Serveur();
		serveur.run();
	}
}
