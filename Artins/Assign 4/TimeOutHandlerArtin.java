/*
 * CPSC 441 Assignment 4
 * Date: 12/2/2016
 * Author: Artin Rezaee
 */


import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

import cpsc441.a4.shared.DvrPacket;

public class TimeOutHandlerArtin extends TimerTask{
	RouterArtin rout;
	
	/**
	 * Constructor
	 * @param r
	 */
	public TimeOutHandlerArtin(RouterArtin r){
		rout = r;
	}

	@Override
	public void run() {
		List<Integer> neighbors = rout.getNeighnors();

	// Send the minCost array of the router to its neighboring routers
		for(int i=0; i<neighbors.size(); i++){
			
			DvrPacket packet = new DvrPacket(rout.getID(), neighbors.get(i) ,DvrPacket.ROUTE ,rout.getMinCost());
			try {
				rout.out.writeObject(packet);
				rout.out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
}
