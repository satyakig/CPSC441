/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 4
 * T-01
 * Timeout.java
 */

import java.io.IOException;
import java.util.TimerTask;
import java.util.Arrays;
import cpsc441.a4.shared.*;


public class Timeout extends TimerTask {
   private Router router;

    /**
     * Constructor to initialize the Timeout instance
     * @param r	Router for the timer
     */
   public Timeout(Router r) {
       router = r;
   }


   // sends all the neighbours of the router a copy of this Router's min cost vector
   @Override
   public void run() {
       // gets the min cost vector of the Router
       int[] min = router.getMinCost();

        // sends all the neighbouring routers the min cost vector
       for(int i = 0; i < router.getNeighbours().size(); i++) {
           DvrPacket pac = new DvrPacket(router.getID(), router.getNeighbours().get(i), DvrPacket.ROUTE, min);
           try {
               router.out.writeObject(pac);
               router.out.flush();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
   }
}
