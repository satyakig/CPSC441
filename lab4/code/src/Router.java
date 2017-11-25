/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 4
 * T-01
 * Router.java

 * This class can be run as a THREAD as show in in the test driver MAIN method
 * It can also be run as a normal class, using the test driver provided by the prof

 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
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
import java.util.ArrayList;
import java.util.Timer;
import cpsc441.a4.shared.*;


public class Router implements Runnable {

	public int id;
	public String sName;
	public int sPort;
	public int interval;

	public ObjectOutputStream out;
	public ObjectInputStream inp;
	public Socket socket;

	public int [] linkCost;
	public int [] nextHop;
	public int [][] minCost;
	public ArrayList<Integer> neighbours;
	public int numRouters;

	public File file;
	public PrintWriter printer;

    /**
     * Constructor to initialize the router instance
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		this.id = routerId;
		this.sName = serverName;
		this.sPort = serverPort;
		this.interval = updateInterval;

		try {
			this.socket = new Socket(sName, sPort);
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.inp = new ObjectInputStream(socket.getInputStream());

			this.file = new File("Router#" + this.id + " Log.txt");
			this.printer = new PrintWriter(this.file);

			printer.printf("Starting Router #%d with parameters:\n", routerId);
			printer.printf("Relay server host name: %s\n", serverName);
			printer.printf("Relay server port number: %d\n", serverPort);
			printer.printf("Routing update interval: %d (milli-seconds)\n", updateInterval);
		}catch(IOException err) {
			err.printStackTrace();
			System.out.println(err.getMessage());
			System.exit(0);
		}
	}
	
	@Override
	public void run() {
		this.start();
	}

    /**
     * starts the router
     * @return The forwarding table of the router
     */
	public RtnTable start() {

		try {
			this.out.writeObject(new DvrPacket(this.id, DvrPacket.SERVER, DvrPacket.HELLO));
			this.out.flush();

			DvrPacket shake = (DvrPacket) this.inp.readObject();
			System.out.println(shake.toString());
			printer.println(shake.toString());

			if(shake.type != DvrPacket.HELLO) {
				System.out.println("Handshake incomplete!");
				printer.println("Handshake incomplete!");
				return null;
			}
			else
				this.server(shake);

			Timer timer = new Timer(true);
			timer.scheduleAtFixedRate(new Timeout(this), (long) this.interval, (long) this.interval);

			int num = 1;
			while(true) {
				DvrPacket rec = (DvrPacket) this.inp.readObject();

				if(rec.type == DvrPacket.QUIT)
					break;
				this.processDvr(rec, num);
				num++;
			}

			if(timer != null)
				timer.cancel();

			this.inp.close();
			this.out.close();
			this.socket.close();
		}catch(Exception err) {
			System.out.println(err.getMessage());
			err.printStackTrace();
		}

		RtnTable table = new RtnTable(this.minCost[this.id], this.nextHop);
		this.printer.println("Router terminated normally");
		this.printer.println();
		this.printer.println("Routing Table at Router #" + this.id);
		this.printer.print(table.toString());
		this.printer.close();

		return table;
	}

	private void processDvr(DvrPacket dvr, int num) {
		if(dvr.sourceid == DvrPacket.SERVER) {
			this.printer.println("Link cost changed: " + dvr.toString());
			System.out.println("Link cost changed: " + dvr.toString());
			this.server(dvr);
		}
		else {
			this.printer.println(dvr.toString());
			System.out.println(dvr.toString());
			this.neighbour(dvr);
		}
	}

	private void server(DvrPacket rcv) {
		this.numRouters = rcv.getMinCost().length;
		this.linkCost = rcv.getMinCost();
		this.nextHop = new int [rcv.getMinCost().length];
		this.minCost = new int [rcv.getMinCost().length] [rcv.getMinCost().length];
		this.neighbours = new ArrayList<>();

		for(int i = 0; i < this.numRouters; i++) {
			if(this.linkCost[i] != DvrPacket.INFINITY && this.linkCost[i] != 0)
				this.neighbours.add(i);
		}

		for(int i = 0; i < this.numRouters; i++) {
			for(int j = 0; j < this.numRouters; j++) {
				if(i == this.id)
					this.minCost[i][j] = this.linkCost[j];
				else
					this.minCost[i][j] = DvrPacket.INFINITY;
			}
		}

		for(int i = 0; i < this.numRouters; i++){
			if(this.linkCost[i] != DvrPacket.INFINITY)
				this.nextHop[i] = i;
			else
				this.nextHop[i] = -999;
		}
	}

	private void neighbour(DvrPacket rcv) {
		int [] tmpMinCost = rcv.getMinCost();

		for(int i = 0; i < this.numRouters; i++)
			this.minCost[rcv.sourceid][i] = tmpMinCost[i];

		for(int i = 0; i < this.numRouters; i++){
			if(i != this.id)
				this.minCost[this.id][i] = findDistance(i);
			else
				this.minCost[this.id][i] = 0;
		}
	}

	private int findDistance(int x) {
		ArrayList<Integer> results = new ArrayList<Integer>();
		ArrayList<Integer> routerIDs = new ArrayList<Integer>();

		int temp = this.minCost[this.id][x];
		results.add(temp);

		if(this.neighbours.contains(x))
			routerIDs.add(x);
		else
			routerIDs.add(-999);

		for(int i = 0; i < this.neighbours.size(); i++){
			if(this.neighbours.get(i) != x){
				int neighbor = this.neighbours.get(i);
				temp = minCost[this.id][neighbor]+ minCost[neighbor][x];
				results.add(temp);				routerIDs.add(neighbor);
			}
		}

		int smallest = results.get(0);
		int nextHop = -999;
		for(int i = 1; i < results.size(); i++){
			if(smallest > results.get(i)){
				smallest = results.get(i);
				nextHop = routerIDs.get(i);
			}
		}

		if(smallest < DvrPacket.INFINITY && nextHop != -999)
			this.nextHop[x] = nextHop;

		return smallest;
	}

	private void printAllTables() {
		System.out.println("Link Cost");
		for(int i = 0; i < this.numRouters; i++) {
			System.out.print(this.linkCost[i] + "  ");
		}
		System.out.println("\n\n");


		System.out.println("Next Hop");
		for(int i = 0; i < this.nextHop.length; i++) {
			System.out.print(this.nextHop[i] + "  ");
		}
		System.out.println("\n\n");


		System.out.println("Min Cost");
		for(int i = 0; i < this.minCost.length; i++) {
			for(int j = 0; j < this.minCost[i].length; j++) {
				System.out.print(this.minCost[i][j] + "  ");
			}
			System.out.println();
		}
		System.out.println("\n\n");

		System.out.println("Neighbours");
		for(int i = 0; i < this.neighbours.size(); i++)
			System.out.println(this.neighbours.get(i) + "  ");
		System.out.println("\n\n");
	}

	public static void main(String[] args) {

//		default parameters
		int routerId = 0;
		String serverName = "192.168.1.71";
		int serverPort = 8887;
		int updateInterval = 1000; //milli-seconds
		
//		if (args.length == 4) {
//			routerId = Integer.parseInt(args[0]);
//			serverName = args[1];
//			serverPort = Integer.parseInt(args[2]);
//			updateInterval = Integer.parseInt(args[3]);
//		} else {
//			System.out.println("incorrect usage, try again.");
//			System.exit(0);
//		}
			
//		print the parameters
//		System.out.printf("Starting Router #%d with parameters:\n", routerId);
//		System.out.printf("Relay server host name: %s\n", serverName);
//		System.out.printf("Relay server port number: %d\n", serverPort);
//		System.out.printf("Routing update interval: %d (milli-seconds)\n", updateInterval);
		
//		start the router
//		the start() method blocks until the router receives a QUIT message
//		Router router = new Router(routerId, serverName, serverPort, updateInterval);
//		RtnTable rtn = router.start();
//		System.out.println("Router terminated normally");

//		print the computed routing table
//		System.out.println();
//		System.out.println("Routing Table at Router #" + routerId);
//		System.out.print(rtn.toString());


		for(int i = 0; i < 4; i++) {
			Router router = new Router(i, serverName, serverPort, updateInterval);
			Thread thread = new Thread(router);
			thread.start();
		}
	}
}
