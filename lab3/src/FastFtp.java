/**
 * FastFtp class
 * @author Satyaki Ghosh
 *         Nov 9 2017
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.Timer;
import java.net.Socket;
import java.net.DatagramSocket;
import java.io.File;

public class FastFtp {

	private TxQueue que;
	private Timer timer;

	private Socket TCPSocket;
	private DatagramSocket UDPSocket;

	private int windowSize;
	private int rtoTimer;

	private String TCPServerName;
	private int TCPServerPort;
	private int UDPServerPort;

	private String fileName;
	private long fileSize;

	/**
     * Constructor to initialize the program
     * @param windowSize	Size of the window for Go-Back_N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(int windowSize, int rtoTimer) {
		this.windowSize = windowSize;
		this.rtoTimer = rtoTimer;

		this.que = new TxQueue(this.windowSize);
		this.timer.schedule(new TimeoutHandler(), this.rtoTimer);

		try {
			this.UDPSocket = new DatagramSocket();
		}catch(SocketException e) {
			System.out.println("Could not open a UDP Socket.\n" + e.getMessage());
			System.exit(0);
		}
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
     * @param fileName		Name of the file to be transferred to the remote server
     */
	public void send(String serverName, int serverPort, String fileName) {

		this.TCPServerName = serverName;
		this.TCPServerPort = serverPort;
		this.fileName = fileName;

		File file = new File(this.fileName);

		if(!file.exists()){
			System.out.println(this.fileName + " does not exist!");
			System.exit(0);
		}

		this.fileSize = file.length();

		try{
			this.TCPSocket = new Socket(this.TCPServerName, this.TCPServerPort);
			DataOutputStream outStream = new DataOutputStream(this.TCPSocket.getOutputStream());
			DataInputStream inpStream = new DataInputStream(this.TCPSocket.getInputStream());

			outStream.writeUTF(this.fileName);
			outStream.writeLong(this.fileSize);
			outStream.writeInt(this.TCPServerPort);
			outStream.flush();

		}catch(IOException e) {
			System.out.println("Could not open a TCP connection with server.\n" + e.getMessage());
			System.exit(0);
		}
	}
	
	
    // A simple test driver
	public static void main(String[] args) {

		// all arguments should be provided
		// as described in the assignment description 
		if (args.length != 5) {
			System.out.println("incorrect usage, try again.");
			System.out.println("usage: FastFtp server port file window timeout");
			System.exit(1);
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
		System.out.println("file transfer completed.");
	}
}
