/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 3
 * T-01
 * ParentTimer.java
 */

import cpsc441.a3.shared.TxQueue;
import java.net.InetAddress;
import java.util.Timer;
import java.net.DatagramSocket;

/*
 * ParentTimer class
 * usable by all classes to start and cancel timers
 */
public class ParentTimer {

    private static TxQueue que;

    private static Timer timer;
    private static long delay;

    private static DatagramSocket UDPSocket;
    private static InetAddress serverAddress;
    private static int serverPort;

    /**
     * sets up the ParentTimer class
     * @param queue transmission queue that holds all the segments to be sent to the server
     * @param soc UDP client socket, used to send all the segments to the server
     * @param add INETAddress for the server
     * @param p	port# of the UDP server socket
     * @param d delay for the timer
     */
    public static void setup(TxQueue queue, DatagramSocket soc, InetAddress add, int p, long d) {

        // stores all the arguments
        que = queue;
        UDPSocket = soc;
        serverAddress = add;
        serverPort = p;
        delay = d;

        // initially no timer is set
        timer = null;
    }

    // starts a new timer
    public static synchronized void setTimer() {
        cancelTimer();

        timer = new Timer(true);
        timer.schedule(new Timeout(que, UDPSocket, serverAddress, serverPort), delay);
    }

    // cancels the current timer
    public static synchronized void cancelTimer() {
        if(timer != null)
            timer.cancel();
    }
}
