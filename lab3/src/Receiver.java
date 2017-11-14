/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 3
 * T-01
 * Receiver.java
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import cpsc441.a3.shared.*;


/*
 * Receiver Runnable class
 * reads all the ACK sent by the server
 * removes elements from the que if they have been ACKed
 */
public class Receiver implements Runnable {

    // transmission queue with all the segments
    private TxQueue que;

    // client UDP socket
    private DatagramSocket UDPSocket;

    // boolean that specifies if a file has been fully read and converted to segments
    private volatile boolean eof;

    // header size of a segment
    private final static int HEADER_SIZE = 4;

    /**
     * Constructor to initialize the Receiver
     * @param soc UDP client socket, used to read the ACKs sent by the server
     * @param queue transmission queue that holds all the segments to be sent to the server
     */
    public Receiver(DatagramSocket soc, TxQueue queue) {

        // stores all the arguments in the constructor
        que = queue;
        UDPSocket = soc;

        // end of file has initially not been reached i.e. false
        eof = false;
    }


    /*
     * while the file to be sent has not been read fully and the transmission que is not empty,
     * reads data received from the server (ACKs)
     * when an ACK is received, creates a segment from the data and calls the processACK method
     */
    @Override
    public void run() {
        while(true) {
            try {
                // creates a byte array with the HEADER_SIZE
                byte[] data = new byte[HEADER_SIZE];
                DatagramPacket packet = new DatagramPacket(data, data.length);

                // gets the packet sent by the server
                UDPSocket.receive(packet);

                // processes the packet
                this.processACK(new Segment(data));

                // if eof is reached and the transmission queue is empty,
                // the thread's task is complete
                if(eof && que.isEmpty())
                    break;

            }catch(IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }catch(InterruptedException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }


    /*
     * if ACK is not in current window, do nothing
     * otherwise:
     * cancel the timer
     * remove all the segments that are ACKed by this ACK from the transmission queue
     * if there are any pending segments in the transmission queue, start the timer
     */
    public synchronized void processACK(Segment seg) throws InterruptedException{

        System.out.println("received ack# " + seg.getSeqNum());

        if(que.element().getSeqNum() <= seg.getSeqNum()) {

            ParentTimer.cancelTimer();

            while(que.element() != null && que.element().getSeqNum() < seg.getSeqNum())
                que.remove();

            if(!que.isEmpty())
                ParentTimer.setTimer();
        }
    }

    // sets the EOF to true i.e. file has been fully read and all segments are in the transmission queue
    public void fileRead() {
        eof = true;
    }

}
