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
	
	/*
	 * Application serveur
	 */
	
	private static String validationIP(BufferedReader reader) {
		//Demande IP
		System.out.println("Veuillez entrez l'adresse IP du serveur : ");
		//Initialisation de variable
		String serverAddress = "";
		boolean valid = false;
		String IP_PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
		while (!valid) {
			//Donnees entree par la console
			try {	
				serverAddress = reader.readLine();
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			//Vérification de la validité des données
			if(serverAddress.matches(IP_PATTERN)) {
				break;
			}
			System.out.println("Adresse IP entree invalide! Veuillez entre une adresse du format XXX.XXX.XXX.XXX : ");
		}
		
		return serverAddress;
	}

	private static int validationPort(BufferedReader reader) {
		//Demande Port
		System.out.println("Veuillez entrez le port d'ecoute du serveur : ");
		int serverPort = 0;
		while( serverPort<5000 || serverPort>5050 ) 		
		{
			//Donnees entrees par la console
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
			//Fermeture du socket d'ecoute
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
				System.out.println("Erreur lors de la connexion, veuillez reessayer!");
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
					
					//Pour tous les clients, TODO :ajouter filtre par apres
					for(ClientHandler client : Serveur.clients) {
						client.getOutput().write(message);
						client.getOutput().newLine();
						client.getOutput().flush();
					}
					
					if(received == "logout") {
						isLoggedIn=false;
					}
					
					
					
				} catch (Exception e) {
					System.out.println("La connexion a ete coupee avec " + this.username + "!");
					isLoggedIn=false;
					clients.remove(this);
				}
			}
			try {
				//Relacher les ressources
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
