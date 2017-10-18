/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 2
 * T-01
 */

import java.util.ArrayList;
import java.util.Scanner;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;


public class WebServer extends Thread {

	private volatile boolean shutdown = false;
	private ServerSocket server;

    /**
     * Default constructor to initialize the web server     *
     * @param port 	The server port at which the web server listens > 1024
     */
	public WebServer(int port) {
		try {
			this.server = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    /*
     * 	The main loop of the web server
     *   Opens a server socket at the specified server port
	 *   Remains in listening mode until shutdown signal	 *
     */
	public void run() {
		ArrayList<WorkerThread> threadPool = new ArrayList<>();
		try{
			server.setSoTimeout(1000);
			while(!shutdown) {
				try{
					Socket socket = this.server.accept();
					WorkerThread worker = new WorkerThread(socket);
					worker.start();
					threadPool.add(worker);
				}catch(SocketTimeoutException e){}
			}
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}

		//waits for all the threads to finish before closing the server
		try {
			for(int i = 0; i < threadPool.size(); i++)
				threadPool.get(i).join();
			this.server.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	
    /*
     * Signals the server to shutdown.
     */
	public void shutdown() {
		shutdown = true;
	}

	public static void main(String[] args) {
		int serverPort = 2225;

		// parse command line args
		if (args.length == 1) {
			serverPort = Integer.parseInt(args[0]);
		}
		
		if (args.length >= 2) {
			System.out.println("wrong number of arguments");
			System.out.println("usage: WebServer <port>");
			System.exit(0);
		}
		
		System.out.println("starting the server on port " + serverPort);
		
		WebServer server = new WebServer(serverPort);
		
		server.start();
		System.out.println("server started. Type \"quit\" to stop");
		System.out.println(".....................................");

		Scanner keyboard = new Scanner(System.in);
		while(!keyboard.next().equals("quit") );
		
		System.out.println();
		System.out.println("shutting down the server...");
		server.shutdown();
		System.out.println("server stopped");
	}
	
}
