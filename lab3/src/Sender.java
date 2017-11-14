/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 3
 * T-01
 * Sender.java
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;
import cpsc441.a3.shared.*;


/*
 * Sender Runnable class
 * reads a file and turns it into a byte array
 * creates segments out of the bytes read from the file
 * adds all the segments to the que and sends them to the server
 */
public class Sender implements Runnable {

    private Receiver receiver;  // reference to the receiver class

    private TxQueue que;    // transmission queue with all the segments

    private DatagramSocket UDPSocket;   // client UDP socket
    private InetAddress serverAddress;  // INETAddress for the server
    private int serverPort;             // UDP server port #

    private File file;      // reference to the file to be sent
    private byte[] data;    // all the bytes in the file
    private int[] splits;   // how many bytes will be in each segment
    private int dataIndex;  // index of last byte to be converted to a segment

    // max payload size for a packet
    public final static int MAX_PAYLOAD_SIZE = 1000;

    /**
     * Constructor to initialize the Sender
     * @param soc UDP client socket, used to send all the segments to the server
     * @param add INETAddress for the server
     * @param p	port# of the UDP server socket
     * @param queue transmission queue that holds all the segments to be sent to the server
     * @param f filename of the file to be sent
     */
    public Sender(DatagramSocket soc, InetAddress add, int p, TxQueue queue, String f) {
        try {
            que = queue;
            UDPSocket = soc;
            serverPort = p;
            serverAddress = add;

            // creates a file object from the filename
            file = new File(f);

            // reads all the data in the file
            data = Files.readAllBytes(Paths.get(file.getPath()));
            // initial index of last byte read is 0
            dataIndex = 0;

            // finds how many segments need to be created and how many bytes will be in each segment
            int quo = data.length / MAX_PAYLOAD_SIZE;
            int rem = data.length % MAX_PAYLOAD_SIZE;

            if(rem != 0 && quo != 0)
                splits = new int[quo + 1];
            else if(rem != 0 && quo == 0)
                splits = new int[1];
            else
                splits = new int[quo];

            if(rem != 0 && quo != 0) {
                int i;
                for(i = 0; i < splits.length - 1; i++) {
                    splits[i] = (i + 1) * 1000;
                }
                splits[i] = i * 1000 + rem;
            }
            else if(rem != 0 && quo == 0)
                splits[0] = rem;
            else {
                for(int i = 0; i < splits.length; i++)
                    splits[i] = (i + 1) * 1000;
            }

        }catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    /*
     * while the file to be sent has not been turned into segments fully,
     * creates, sends and adds the segment to the transmission queue
     */
    @Override
    public void run() {
        try {
            for(int i = 0; i < splits.length; i++) {
                byte[] payload;
                if(i == 0)
                    payload = new byte[splits[i]];
                else
                    payload = new byte[splits[i] - splits[i - 1]];

                // creates a payload
                for(int j = 0; j < payload.length; j++, dataIndex++)
                    payload[j] = data[dataIndex];

                // if the transmission que is full, sleep for 500ms
                while(que.isFull())
                    Thread.sleep(500);

                // add segment to the transmission queue
                Segment seg = new Segment(i, payload);

                // send the segment
                this.processSend(seg);
            }

            // tell the Sender that all the segments of the file has been added to the transmission queue
            receiver.fileRead();

        }catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * sends seg to the UDP socket
     * add seg to the transmission queue
     * if this is the first segment in the transmission queue, start the timer
     */
    public synchronized void processSend(Segment seg) {

        try {
            System.out.println("sending seq# " + seg.getSeqNum());

            // creates and sends a packet to the server
            DatagramPacket packet = new DatagramPacket(seg.getBytes(), seg.getBytes().length, serverAddress, serverPort);
            UDPSocket.send(packet);

            // adds seg to the transmission queue
            que.add(seg);

            // starts the timer if this is the first segment in the queue
            if(que.size() == 1)
                ParentTimer.setTimer();

        }catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    // sets the reference to the Receiver object
    public void setReceiver(Receiver t) {
        receiver = t;
    }

}
