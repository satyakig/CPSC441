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
import java.net.*;
import java.util.*;
import cpsc441.a4.shared.*;


public class Router {

	private int id;			// ID of the Router
	private String sName;	// server host name/ip address
	private int sPort;		// server port number
	private int interval;	// interval to send out updates

	private Socket socket;				// TCP socket
	public ObjectOutputStream out;		// output stream to socket
	private ObjectInputStream inp;		// input stream to socket

	private int [] linkCost;	//link cost array
	private int [] nextHop;		// next hop array
	private int [][] minCost;	// min cost array
	private int numRouters;		// number of total routers in the system
	private ArrayList<Integer> neighbours;	// holds the neighbour router IDs of this router

	private File file;			// file to log the info in
	private PrintWriter printer;	// printer to print to file

	/**
	 * Constructor to initialize the router instance
	 * Opens the TCP connection with the server and
	 * @param routerId			Unique ID of the router starting at 0
	 * @param serverName		Name of the host running the network server
	 * @param serverPort		TCP port number of the network server
	 * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
	 */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		// saves all the arguments and server info
		this.id = routerId;
		this.sName = serverName;
		this.sPort = serverPort;
		this.interval = updateInterval;

		// creates a socket and opens input/output streams with the server info provided
		// opens a file and printing stream to log all the router information to
		try {
			this.socket = new Socket(sName, sPort);
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.inp = new ObjectInputStream(socket.getInputStream());

			// starts logging the router info to the file
			this.file = new File("Router #" + this.id + " Log.txt");
			this.printer = new PrintWriter(this.file);
			printer.printf("Starting Router #%d with parameters:\n", routerId);
			printer.printf("Relay server host name: %s\n", serverName);
			printer.printf("Relay server port number: %d\n", serverPort);
			printer.printf("Routing update interval: %d (milli-seconds)\n\n", updateInterval);

			// sends the first hello message to the server
			this.out.writeObject(new DvrPacket(this.id, DvrPacket.SERVER, DvrPacket.HELLO));
			this.out.flush();

			// receives the hello back from the server
			DvrPacket shake = (DvrPacket) this.inp.readObject();
			System.out.println(shake.toString());
			printer.println(shake.toString());

			// if the received packet does not contain a Hello, the program quits
			if(shake.type != DvrPacket.HELLO) {
				this.error("Handshake incomplete! Server did NOT send HELLO.");
				System.exit(0);
			}
			// initializes the the min cost, next hop, link cost vectors if packet contains HELLO
			else
				this.server(shake);

		}catch(Exception err) {
			this.error(err.getMessage());
			System.exit(0);
		}
	}


	/**
	 * starts the router
	 * @return The forwarding table of the router
	 */
	public RtnTable start() {

		// starts a timer that goes off after the period specified over and over
		// no need to cancel or restart the timer until the program is over
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(new Timeout(this), (long) this.interval, (long) this.interval);

		// keeps receiving packets from the server until it says to QUIT
		while(true) {
			try {
				DvrPacket rec = (DvrPacket) this.inp.readObject();

				// if the packet says quit, then the while loop stops
				if(rec.type == DvrPacket.QUIT)
					break;
				// if packet does not contain QUIT, we process the packet
				else
					this.processDvr(rec);
			}catch(Exception err) {
				// if there are any errors with the socket, closes all the streams, socket and cleans up
				this.error(err.getMessage());
				System.exit(0);
			}
		}

		// cancel the timer after server has QUIT
		if(timer != null)
			timer.cancel();

		// close all the TCP sockets, streams and cleans up
		// router terminated normally
		try {
			this.inp.close();
			this.out.close();
			this.socket.close();
		}catch(Exception err) { }

		// create the RtnTable with the minCost and nextHop
		RtnTable table = new RtnTable(this.minCost[this.id], this.nextHop);

		// prints the info to the logging file
		this.printer.println("Router #" + this.id + " terminated normally");
		System.out.println("Router #" + this.id + " terminated normally");
		this.printer.println();

		this.printAllTables();
		this.printer.println("Routing Table of Router #" + this.id);
		this.printer.print(table.toString());
		this.printer.close();

		// returns the table
		return table;
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
	 * processes the link cost changes sent by the server
	 * resets linkCost, minCost, nextHop and neighbours vectors
	 * @param rcv received packet from the server
	 */
	private void server(DvrPacket rcv) {
		// makes new arrays to hold the info, deletes the old values
		this.numRouters = rcv.getMinCost().length;
		this.linkCost = rcv.getMinCost();
		this.nextHop = new int [this.numRouters];
		this.minCost = new int [this.numRouters] [this.numRouters];
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
		for(int i = 0; i < this.numRouters; i++) {
			if(this.linkCost[i] != DvrPacket.INFINITY)
				this.nextHop[i] = i;
			else
				this.nextHop[i] = -999;
		}
	}


	/**
	 * process the normal min cost updates sent by other routers
	 * updates minCost and nextHop vectors
	 * @param rcv received packet from the server
	 */
	private void neighbour(DvrPacket rcv) {
		// copy the received min cost vector into the current min cost vector for that router
		int [] rcvMinCost = rcv.getMinCost();
		for(int i = 0; i < this.numRouters; i++)
			this.minCost[rcv.sourceid][i] = rcvMinCost[i];

		// compute the new min cost based on the received information
		for(int i = 0; i < this.numRouters; i++) {
			if(i != this.id)
				this.minCost[this.id][i] = findDistance(i);
			else
				this.minCost[this.id][i] = 0;
		}
	}

	/**
	 * @param routTo ID of router to calculate the smallest distance to
	 * @return smallest distance from this router to the router with ID = routTo
	 */
	private synchronized int findDistance(int routTo) {
		int curDist = this.minCost[this.id][routTo];
		int hopTo = -999;
		int distanceCalc = DvrPacket.INFINITY;


		// finds the distance to router with ID: routTo using the neighbour routers as a middle man
		for(int i = 0; i < this.neighbours.size(); i++) {
			int n = this.neighbours.get(i);
			int d = this.linkCost[n] + this.minCost[n][routTo];
			if(d < curDist) {
				hopTo = n;
				distanceCalc = d;
			}
		}

		// if the distance calculated is NOT less than the current distance, then return the current distance
		if(hopTo == -999 || distanceCalc == DvrPacket.INFINITY)
			return curDist;

		// if the distance calculated is less than the current distance, then return the distance calculated
		// update the nextHop vector with the router to hop to
		this.nextHop[routTo] = hopTo;
		return distanceCalc;
	}

	// prints all the tables i.e the linkCost, nextHop and neighbours to a file
	private void printAllTables() {

		this.printer.println(this.numRouters + " routers in the system.\n");
		this.printer.println("Link Cost Table of Router #" + this.id);
		for(int i = 0; i < this.numRouters; i++) {
			this.printer.print(this.linkCost[i] + "    ");
		}
		this.printer.println("\n");

		this.printer.println("Next Hop Table of Router #" + this.id);
		for(int i = 0; i < this.numRouters; i++) {
			this.printer.print(this.nextHop[i] + "    ");
		}
		this.printer.println("\n");

		this.printer.println("Neighbours of Router #" + this.id);
		for(int i = 0; i < this.neighbours.size(); i++)
			this.printer.print("R" + this.neighbours.get(i) + "    ");
		this.printer.println("\n");

		this.printer.println("Min Cost Table of Router #" + this.id);
		for(int i = 0; i < this.numRouters; i++) {
			for(int j = 0; j < this.numRouters; j++)
				this.printer.print(this.minCost[i][j] + "   ");
			this.printer.println();
		}
		this.printer.println("\n");
	}

	// return this router's ID
	public int getID() {
		return this.id;
	}

	// returns an array of this router's neighbours
	public ArrayList<Integer> getNeighbours() {
		return this.neighbours;
	}

	// returns a copy of the min cost array
	public int[] getMinCost() {
		return Arrays.copyOf(this.minCost[id], this.numRouters);
	}

	// prints errors and terminates TCP connections
	private void error(String message) {
		try {
			System.out.println("Router #" + this.id + " terminated with: " + message);
			printer.println("Router #" + this.id + " terminated with: " + message);
			inp.close();
			out.close();
			socket.close();
			printer.close();
		}catch(Exception err) { }
	}


	// simple test driver
	public static void main(String[] args) {

		// default parameters
		int routerId = 2;
		String serverName = "localhost";
		int serverPort = 8887;
		int updateInterval = 1000; //milli-seconds

		if (args.length == 4) {
			try {
				routerId = Integer.parseInt(args[0]);
				serverName = args[1];
				serverPort = Integer.parseInt(args[2]);
				updateInterval = Integer.parseInt(args[3]);
			}catch(Exception err){
				System.out.println(err.getMessage());
				System.exit(0);
			}
		} else {
			System.out.println("Incorrect usage, try again. Need 4 arguments to run Router.");
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

		// print the computed routing table
		System.out.println();
		System.out.println("Routing Table of Router #" + routerId);
		System.out.print(rtn.toString());
	}
}
