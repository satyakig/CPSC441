/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 3
 * T-01
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;
import cpsc441.a3.shared.*;


/*
 * Timeout Runnable class
 * sends all the segments in the que if the timer goes off
 */
public class Timeout extends TimerTask {

    private TxQueue que;    // transmission queue with all the segments

    private DatagramSocket UDPSocket;   // client UDP socket
    private InetAddress serverAddress;  // INETAddress for the server
    private int serverPort;             // UDP server port

    /**
     * Constructor to initialize the Timeout
     * @param q	transmission queue that holds all the segments
     * @param soc UDP client socket, used to send the segments in the queue to the server
     * @param add INETAddress of the server
     * @param p	port# of the UDP server socket
     */
    public Timeout(TxQueue q, DatagramSocket soc, InetAddress add, int p) {
        que = q;
        UDPSocket = soc;
        serverAddress = add;
        serverPort = p;
    }


    /*
     * gets the list of all pending segments from the transmission queue
     * go through the list and send all segments to the UDP socket
     * if there are any pending segments in the transmission queue, start the timer
     */
    @Override
    public void run() {
        try {
            // gets all the segments that are in the queue/window
            Segment[] segments = que.toArray();

            // creates packets from the segments and sends them to the server
            for(int i = 0; i < segments.length; i++) {

                Segment seg = segments[i];
                DatagramPacket packet = new DatagramPacket(seg.getBytes(), seg.getBytes().length, serverAddress, serverPort);
                UDPSocket.send(packet);

                System.out.println("timeout sending seq# " + seg.getSeqNum());
            }

            if(!que.isEmpty())
                ParentTimer.setTimer();

        }catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
