/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 3
 * T-01
 * FastFtp.java
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.DatagramSocket;
import java.io.File;
import cpsc441.a3.shared.*;

public class FastFtp {

	private TxQueue que; // holds the Queue

	private Socket TCPSocket;		// TCP socket
	private DatagramSocket UDPSocket;	//UDP socket
	private int TCPServerPort;	// TCP server port #
	private int UDPServerPort;	// UDP server port #
	private InetAddress serverAddress;	// server INETAddress

	private String fileName;	// filename to transfer
	private long fileSize;		// size of the file

	private long delay;	// delay for the timeout


	/**
     * Constructor to initialize the program
     * @param windowSize	Size of the window for Go-Back_N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(int windowSize, int rtoTimer) {

		// gets the LONG value of the rtoTimer integer
		delay = Long.valueOf(rtoTimer);

		// creates a que with the window size specified
		que = new TxQueue(windowSize);
	}
	

    /**
     * Sends the specified file to the specified destination host:
     * 1. send file/connection info over TCP
     * 2. start receiving thread to process coming ACKs
     * 3. send file segment by segment
     * 4. wait until transmit queue is empty, i.e., all segments are ACKed
     * 5. clean up (cancel timer, interrupt receiving thread, close sockets/files)
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileN		Name of the file to be transferred to the remote server
     */
	public void send(String serverName, int serverPort, String fileN) {

		// reads the arguments of the function and stores them
		TCPServerPort = serverPort;
		fileName = fileN;

		// creates a file object from the filename specified
		File file = new File(fileName);

		// if the file does not exist the program quits
		if(!file.exists()){
			System.out.println(fileName + " does not exist!");
			System.exit(0);
		}

		// if the file exists, reads the file length
		this.fileSize = file.length();

		try{
			// opens a TCP and UDP socket
			TCPSocket = new Socket(serverName, TCPServerPort);
			UDPSocket = new DatagramSocket();

			// creates the input and output streams for the TCP socket
			DataOutputStream outStream = new DataOutputStream(TCPSocket.getOutputStream());
			DataInputStream inpStream = new DataInputStream(TCPSocket.getInputStream());

			// writes and sends the filename, file size and local UDP port# to the server
			outStream.writeUTF(fileName);
			outStream.writeLong(fileSize);
			outStream.writeInt(UDPSocket.getLocalPort());
			outStream.flush();

			// reads the UDP port# of the server, that it sends back
			UDPServerPort = inpStream.readInt();
			System.out.println("TCP Server: " + serverName + ", Port: " + serverPort);
			System.out.println("UDP Server: " + serverName + ", Port: " + UDPServerPort + "\n");

			// converts the server hostname to an INETAddress
			serverAddress = InetAddress.getByName(serverName);

			// setup the ParentTimer class
			ParentTimer.setup(que, UDPSocket, serverAddress, UDPServerPort, delay);

			// creates a receiver and sender runnable object
			Sender sender = new Sender(UDPSocket, serverAddress, UDPServerPort, que, fileName);
			Receiver receiver = new Receiver(UDPSocket, que);

			// sends the sender a reference to the receiver object
			sender.setReceiver(receiver);

			// creates two threads with the sender and receiver objects
			Thread senderT = new Thread(sender);
			Thread receiverT = new Thread(receiver);

			// starts the two threads
			senderT.start();
			receiverT.start();

			// waits for the two threads to finish doing their tasks
			senderT.join();
			receiverT.join();

			// after file has been sent and all ACKs have been received, the TCP and UDP sockets are closed
			TCPSocket.close();
			UDPSocket.close();

		}catch(IOException e) {
			System.out.println("Socket error. " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}catch(InterruptedException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	
    // A simple test driver
	public static void main(String[] args) {

		// all arguments should be provided
		// as described in the assignment description 
		if (args.length != 5) {
			System.out.println("incorrect usage, try again.");
			System.out.println("usage: FastFtp server port file window timeout");
			System.exit(0);
		}
		
		// parse the command line arguments
		// assume no errors
		String serverName = args[0];
		int serverPort = Integer.parseInt(args[1]);
		String fileName = args[2];
		int windowSize = Integer.parseInt(args[3]);
		int timeout = Integer.parseInt(args[4]);


		// send the file to server
		FastFtp ftp = new FastFtp(windowSize, timeout);
		System.out.printf("sending file \'%s\' to server...\n", fileName);

		ftp.send(serverName, serverPort, fileName);
		System.out.println("\nfile transfer completed.");
	}
}
