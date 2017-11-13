/*
 * CPSC 441 Assignment 4
 * Date: 12/2/2016
 * Author: Artin Rezaee
 */

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import cpsc441.a4.shared.*;


/**
 * Router Class
 * 
 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
 * 
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 * 
 * A separate process broadcasts routing update messages
 * to directly connected neighbors at regular intervals.
 * 
 *      
 * @author 	Majid Ghaderi
 * @version	2.1
 *
 */
public class Router {
	
	
	private int ID , serverPort, updateInterval;
	private String serverName;
	private List<Integer> neighbors = new ArrayList<Integer>();
	public Socket socket;
	public int [] LinkCost;
	public int [] nextHop;
	public int [][] minCost;
	public ObjectOutputStream out  = null;
	public ObjectInputStream in = null;
	
    /**
     * Constructor to initialize the router instance 
     * 
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		ID = routerId;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.updateInterval = updateInterval;
	
	// Creating a TCP connection to the server
		try {
			socket = new Socket(serverName, serverPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

    /**
     * starts the router 
     * 
     * @return The forwarding table of the router
     */
	public RtnTable start() {
		
		RtnTable rtnTbl = null;
		
	// Creating the Hello Process to send to the relay server 
		DvrPacket Hello = new DvrPacket(this.ID, DvrPacket.SERVER, DvrPacket.HELLO);
	
	// Sending the Hello process over the TCP connection to the relay server
		try {
			out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(Hello);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	// Getting the Hello response 
		DvrPacket Response = null;
		try {
			in = new ObjectInputStream(socket.getInputStream());
		
	// Wait until the response is Hello
		Response = (DvrPacket) in.readObject();
		
	// check to see if Hello message is received
		if(Response.type != DvrPacket.HELLO)
			return null;
	
	// Initialize the linkCost, nextHop, and minCost arrays  get next hop from link cost
		this.initializeData(Response.getMinCost());

		
	} catch (Exception e) {
		e.printStackTrace();
	}

	
	// Start the timer
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimeOutHandler(this), this.updateInterval, this.updateInterval);
		
		DvrPacket packet = null;
		
		while(true){
	
	// Read datagram from other routers
			try {
				packet = (DvrPacket) in.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
	// Check to see if the received message is a quit message. If so, terminate the program
			if(packet.type == DvrPacket.QUIT){
				break;
			}

	// Call the processDvr method to compute the new distance vector
			this.processDvr(packet);

		}	
		
	// Canceling the timer, closing the socket, and Cleaning up
		timer.cancel();
		try {
			in.close();
			socket.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	// Create the routing table and return
		int[] temp = this.getMinCost();
		rtnTbl = new RtnTable(temp, this.nextHop);
	
		return rtnTbl;
	}

	
	/**
	 * initializes linkCost, minCost, and nexthop arrays of this class 
	 * @param received
	 */
	private void initializeData(int[] received){
		
		this.LinkCost = received;
		this.minCost = new int[this.LinkCost.length][this.LinkCost.length];
		this.nextHop = new int [this.LinkCost.length];
	
	// Copy linkCost entries into the minCost array 
		for(int i=0; i<this.minCost[this.ID].length; i++){
			this.minCost[this.ID][i] = this.LinkCost[i];
		}
		
	// populate the rest of the minCost array with infinity
		for(int i=0; i<this.LinkCost.length; i++){
			if(i != this.ID){
				for(int j=0; j<this.LinkCost.length; j++)
					this.minCost[i][j] = DvrPacket.INFINITY;
			}
		}
		
	// Determine neighboring routers and set them into the nextHop array 
		for(int i=0; i<this.LinkCost.length; i++){
			if(this.LinkCost[i] != DvrPacket.INFINITY){
				this.nextHop[i] = i;
			}
			else
				this.nextHop[i] = -1;
		}
		
	// Populate the neighbors list for easier access to the members
		for(int i=0; i<this.LinkCost.length; i++){
			if(this.LinkCost[i] != DvrPacket.INFINITY && this.LinkCost[i] != 0){
				this.neighbors.add(i);
			}
		}
	}
	
	
	/**
	 * Implements the modified Bellman-Ford algorithm. The operation of this method
	 * depends on the Sender of the DvrPacket
	 * @param Dvr
	 */
	public void processDvr(DvrPacket Dvr){
		
	 // linkCost change. Update the linkCost and minCost arrays
		if(Dvr.sourceid == DvrPacket.SERVER){
			this.initializeData(Dvr.mincost);
		}

	// regular routing update from the neighboring routers. Update minCost 	
		else{
			
	// Copy the received linkCost to the minCost array of the index of sourceID
			int [] rcvLinkCost = Dvr.getMinCost();
			for(int i=0; i<this.LinkCost.length; i++){
				this.minCost[Dvr.sourceid][i] = rcvLinkCost[i];
			}
	// Recompute distance to all other routers
			List<Integer> res = new ArrayList<Integer>();
			for(int i=0; i<this.LinkCost.length; i++){
				if(i != this.ID){
					res.add(computeDV(i));
				}
				else
					res.add(0);
			}
			
	// Copy the resulting calculated paths back to the distance vector
			for(int i=0; i<this.LinkCost.length; i++){
				this.minCost[ID][i] = res.get(i);
			}
	
		}
	}
	
	/**
	 * Method to calculate the distance vector and return the shortest distance from this router to 
	 * a particular router. 
	 * @param dist
	 * @return the smallest path from this router to the router Id indicated in parameter dist
	 */
	private int computeDV(int dist){
		List<Integer> results = new ArrayList<Integer>();
		List<Integer> routerIDs = new ArrayList<Integer>();
		
	// Compute C(r0,dist)+Ddist(dist)
		int temp = this.minCost[this.ID][dist];
		results.add(temp);
		if(this.neighbors.contains(dist))
			routerIDs.add(dist);
		else
			routerIDs.add(-1);
		
	// Equivalent to computing C(r0, neighboring node)+Dneighboring node(dist)
	// This is calculated for all the neighboring nodes
		for(int i=0; i<this.neighbors.size(); i++){
			if(this.neighbors.get(i)!=dist){
				int neighbor = this.neighbors.get(i);
				temp = minCost[ID][neighbor]+ minCost[neighbor][dist];
				results.add(temp);
				routerIDs.add(neighbor);
			}
		}
		
	// from the calculated results choose the smallest one and return it 
		int smallest = results.get(0);
		int nextHop = -1;
		for(int i=1; i<results.size(); i++){
			if(smallest > results.get(i)){
				smallest = results.get(i);
				nextHop = routerIDs.get(i);
			}
		}
		
		
	// Update the nextHop array
		if(smallest < DvrPacket.INFINITY && nextHop != -1){
			this.nextHop[dist] = nextHop;
		}
		
		return smallest;
	}
	
	/**
	 * Gets the ID of this router
	 * @return the ID of this router
	 */
	public int getID(){
		return this.ID;
	}
	
	/**
	 * gets the minCost of only this router
	 * @return an array of int that contains the minCost of this router
	 */
	public int[] getMinCost(){
		return this.minCost[this.ID].clone();
	}
	
	/**
	 * get the neighboring routers to this router
	 * @return a list of all neighboring routers to this router
	 */
	public List<Integer> getNeighnors(){
		return this.neighbors;
	}
	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		// default parameters
		int routerId = 0;
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 100; //milli-seconds
		
		// the router can be run with:
		// i. a single argument: router Id
		// ii. all required arguments
		if (args.length == 1) {
			routerId = Integer.parseInt(args[0]);
		}
		else if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		}
		else {
			System.out.println("incorrect usage, try again.");
			System.exit(0);
		}
			
		// print the parameters
		System.out.printf("starting Router #%d with parameters:\n", routerId);
		System.out.printf("Relay server host name: %s\n", serverName);
		System.out.printf("Relay server port number: %d\n", serverPort);
		System.out.printf("Routing update interval: %d (milli-seconds)\n", updateInterval);
		
		// start the server
		// the start() method blocks until the router receives a QUIT message
		Router router = new Router(routerId, serverName, serverPort, updateInterval);
		RtnTable rtn = router.start();
		System.out.println("Router terminated normally");
		
		// print the computed routing table
		System.out.println();
		System.out.println("Routing Table at Router #" + routerId);
		System.out.print(rtn.toString());
	}

}
