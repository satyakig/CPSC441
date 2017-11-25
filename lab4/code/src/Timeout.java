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

   public Timeout(Router r) {
       router = r;
   }

   @Override
   public void run() {
       int[] min = Arrays.copyOf(router.minCost[router.id], router.minCost[router.id].length);

       for(int i = 0; i < router.neighbours.size(); i++) {
           DvrPacket pac = new DvrPacket(router.id, router.neighbours.get(i), DvrPacket.ROUTE, min);
           try {
               router.out.writeObject(pac);
               router.out.flush();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
   }
}
