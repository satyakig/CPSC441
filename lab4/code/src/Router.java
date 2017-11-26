/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 4
 * T-01
 * Router.java
 *
 * This class implements the functionality of a router
 * when running a modified distance vector routing algorithm.
 *
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 */

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Timer;
import java.net.SocketException;
import cpsc441.a4.shared.*;


public class Router {

	public int id;			// ID of the Router
	public String sName;	// server host name/ip address
	public int sPort;		// server port number
	public int interval;	// interval to send out updates

	public Socket socket;				// TCP socket
	public ObjectOutputStream out;		// output stream to socket
	public ObjectInputStream inp;		// input stream to socket

	public int [] linkCost;		//link cost array
	public int [] nextHop;		// next hop array
	public int [][] minCost;	// min cost array


	public ArrayList<Integer> neighbours;	// holds the neighbour router IDs of this router
	public int numRouters;					// number of total routers in the system

	public File file;			// file to log the info in
	public PrintWriter printer;	// printer to print to file

    /**
     * Constructor to initialize the router instance
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		// saves all the arguments
		this.id = routerId;
		this.sName = serverName;
		this.sPort = serverPort;
		this.interval = updateInterval;

		// creates a socket and opens streams with the server info provided
		// opens a file to log all the router information to
		try {
			this.socket = new Socket(sName, sPort);
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.inp = new ObjectInputStream(socket.getInputStream());

			this.file = new File("Router#" + this.id + " Log.txt");
			this.printer = new PrintWriter(this.file);

			printer.printf("Starting Router #%d with parameters:\n", routerId);
			printer.printf("Relay server host name: %s\n", serverName);
			printer.printf("Relay server port number: %d\n", serverPort);
			printer.printf("Routing update interval: %d (milli-seconds)\n\n", updateInterval);
		}catch(IOException err) {
			err.printStackTrace();
			System.out.println(err.getMessage());
			System.exit(0);
		}
	}


    /**
     * starts the router
     * @return The forwarding table of the router
     */
	public RtnTable start() {

		try {
			// sends the first hello message to the server
			this.out.writeObject(new DvrPacket(this.id, DvrPacket.SERVER, DvrPacket.HELLO));
			this.out.flush();

			// receives the hello back from the server
			DvrPacket shake = (DvrPacket) this.inp.readObject();
			System.out.println(shake.toString());
			printer.println(shake.toString());

			// if the received packet does not contain a Hello, the program quits
			if(shake.type != DvrPacket.HELLO) {
				System.out.println("Handshake incomplete!");
				printer.println("Handshake incomplete!");
				return null;
			}
			else
				this.server(shake); // initializes the the min cost, next hop, link cost vectors

			// starts a timer that goes off after the period specified over and over
			// no need to cancel or restart the timer until the program is over
			Timer timer = new Timer(true);
			timer.scheduleAtFixedRate(new Timeout(this), (long) this.interval, (long) this.interval);

			while(true) {
				// keeps receiving packets from the server until it says to QUIT
				DvrPacket rec = (DvrPacket) this.inp.readObject();

				// if the packet says the quit then the while loop stops
				if(rec.type == DvrPacket.QUIT)
					break;
				// if packet does not contain QUIT, then we process the packet
				else
					this.processDvr(rec);
			}

			// cancel the timer after server has QUIT
			if(timer != null)
				timer.cancel();

			// close all the TCP sockets and streams
			this.inp.close();
			this.out.close();
			this.socket.close();
		}catch(SocketTimeoutException err) {
			// if the socket times out, closes all the streams, socket and cleans up
			System.out.println("Router terminated with a " + err.getMessage());

			try {
				this.inp.close();
				this.out.close();
				this.socket.close();

				this.printer.println("Router terminated with a " + err.getMessage());
				this.printer.println();
				this.printAllTables();
				this.printer.println("Routing Table at Router #" + this.id);
				RtnTable table1 = new RtnTable(this.minCost[this.id], this.nextHop);
				this.printer.print(table1.toString());
				this.printer.close();
				return table1;
			}catch(Exception e) {}
			System.exit(0);

		}catch(SocketException err) {
			// if there are other socket exceptions, closes all the streams, socket and cleans up
			System.out.println("Router terminated with a " + err.getMessage());
			try {
				this.inp.close();
				this.out.close();
				this.socket.close();

				this.printer.println("Router terminated with a " + err.getMessage());
				this.printer.println();
				this.printAllTables();
				this.printer.println("Routing Table at Router #" + this.id);
				RtnTable table1 = new RtnTable(this.minCost[this.id], this.nextHop);
				this.printer.print(table1.toString());
				this.printer.close();
				return table1;
			}catch(Exception e) {}
			System.exit(0);
		}catch(Exception err) {
			System.out.println(err.getMessage());
			err.printStackTrace();
		}

		// create the RtnTable with the minCost and nextHop
		RtnTable table2 = new RtnTable(this.minCost[this.id], this.nextHop);

		// prints the info to the logging file
		this.printer.println("Router terminated normally");
		this.printer.println();
		this.printAllTables();
		this.printer.println("Routing Table at Router #" + this.id);
		this.printer.print(table2.toString());
		this.printer.close();

		// returns the table
		return table2;
	}


	/**
	 * processes the packet received from the server
	 * @param dvr received packet from the server
	 */
	private void processDvr(DvrPacket dvr) {

		// if the packet sourceID is from the server, then it calls the method for processing packets from the server
		// this is a link cost change message
		if(dvr.sourceid == DvrPacket.SERVER) {
			this.printer.println("Link cost changed: " + dvr.toString());
			System.out.println("Link cost changed: " + dvr.toString());
			this.server(dvr);
		}

		// else it calls the method for processing packets from neighbours
		// this is a regular routing update from a neighbour
		else {
			this.printer.println(dvr.toString());
			System.out.println(dvr.toString());
			this.neighbour(dvr);
		}
	}

	/**
	 * updates linkCost, minCost, nextHop, neighbour vectors
	 * @param rcv received packet from the server
	 */
	private void server(DvrPacket rcv) {
		// makes new arrays to hold the info, deletes the old values
		this.numRouters = rcv.getMinCost().length;
		this.linkCost = rcv.getMinCost();
		this.nextHop = new int [rcv.getMinCost().length];
		this.minCost = new int [rcv.getMinCost().length] [rcv.getMinCost().length];
		this.neighbours = new ArrayList<>();

		// adds neighbours of this router to the neighbours vector
		for(int i = 0; i < this.numRouters; i++) {
			if(this.linkCost[i] != DvrPacket.INFINITY && this.linkCost[i] != 0)
				this.neighbours.add(i);
		}

		// initializes the minCost[this.id] array with the link cost values
		// initializes the rest of the array with infinity
		for(int i = 0; i < this.numRouters; i++) {
			for(int j = 0; j < this.numRouters; j++) {
				if(i == this.id)
					this.minCost[i][j] = this.linkCost[j];
				else
					this.minCost[i][j] = DvrPacket.INFINITY;
			}
		}

		// initializes the nextHop vector with the neighbour routerIDs
		for(int i = 0; i < this.numRouters; i++){
			if(this.linkCost[i] != DvrPacket.INFINITY)
				this.nextHop[i] = i;
			else
				this.nextHop[i] = -999;
		}
	}

	/**
	 * updates linkCost, minCost, nextHop, neighbour vectors
	 * @param rcv received packet from the server
	 */
	private void neighbour(DvrPacket rcv) {
		// copy the received min cost vector into the current min cost vector for that router
		int [] tmpMinCost = rcv.getMinCost();
		for(int i = 0; i < this.numRouters; i++)
			this.minCost[rcv.sourceid][i] = tmpMinCost[i];

		// compute the new min cost based on this information
		for(int i = 0; i < this.numRouters; i++) {
			if(i != this.id)
				this.minCost[this.id][i] = findDistance(i);
			else
				this.minCost[this.id][i] = 0;
		}
	}

	/**
	 * @param x ID of router to calculate the smallest distance to
	 * @return smallest distance from this router to the router with ID = x
	 */
	private int findDistance(int x) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		ArrayList<Integer> rout = new ArrayList<Integer>();

		int temp = this.minCost[this.id][x];
		res.add(temp);

		if(this.neighbours.contains(x))
			rout.add(x);
		else
			rout.add(-999);

		for(int i = 0; i < this.neighbours.size(); i++){
			if(this.neighbours.get(i) != x){
				int neighbor = this.neighbours.get(i);
				temp = minCost[this.id][neighbor]+ minCost[neighbor][x];
				res.add(temp);
				rout.add(neighbor);
			}
		}

		int dist = res.get(0);
		int nextHop = -999;
		for(int i = 1; i < res.size(); i++) {
			if(dist > res.get(i)) {
				dist = res.get(i);
				nextHop = rout.get(i);
			}
		}

		if(dist < DvrPacket.INFINITY && nextHop != -999)
			this.nextHop[x] = nextHop;

		// returns the smallest calculated distance
		return dist;
	}


	// prints all the tables i.e the linkCost, nextHop and neighbours
	private void printAllTables() {

		this.printer.println(this.numRouters + " routers in this system.\n");
		this.printer.println("Link Cost at Router #" + this.id);
		for(int i = 0; i < this.numRouters; i++) {
			this.printer.print("R" + i + ": " + this.linkCost[i] + "    ");
		}
		this.printer.println("\n");

		this.printer.println("Next Hop at Router #" + this.id);
		for(int i = 0; i < this.nextHop.length; i++) {
			this.printer.print("R" + i + ": " + this.nextHop[i] + "    ");
		}
		this.printer.println("\n");

		this.printer.println("Neighbours at Router #" + this.id);
		for(int i = 0; i < this.neighbours.size(); i++)
			this.printer.print("R" + this.neighbours.get(i) + "    ");
		this.printer.println("\n\n");
	}

	public static void main(String[] args) {

		// default parameters
		int routerId = 0;
		String serverName = "192.168.1.71";
		int serverPort = 8887;
		int updateInterval = 1000; //milli-seconds
		
		if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		} else {
			System.out.println("incorrect usage, try again.");
			System.exit(0);
		}
			
		// print the parameters
		System.out.printf("Starting Router #%d with parameters:\n", routerId);
		System.out.printf("Relay server host name: %s\n", serverName);
		System.out.printf("Relay server port number: %d\n", serverPort);
		System.out.printf("Routing update interval: %d (milli-seconds)\n\n", updateInterval);
		
		// start the router
		// the start() method blocks until the router receives a QUIT message
		Router router = new Router(routerId, serverName, serverPort, updateInterval);
		RtnTable rtn = router.start();
		if(rtn != null)
			System.out.println("Router terminated normally");

		// print the computed routing table
		System.out.println();
		System.out.println("Routing Table at Router #" + routerId);

		if(rtn != null)
			System.out.print(rtn.toString());
	}
}
