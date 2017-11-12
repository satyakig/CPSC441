/**
 * FastFtp class
 * @author Satyaki Ghosh
 *         Nov 9 2017
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.net.Socket;
import java.net.DatagramSocket;
import java.io.File;

public class FastFtp {
	private TxQueue que;

	private Socket TCPSocket;
	private DatagramSocket UDPSocket;

	private String hostName;
	private int TCPServerPort;
	private int UDPServerPort;

	private String fileName;
	private long fileSize;

	private int window;
	private long delay;

	/**
     * Constructor to initialize the program
     * @param windowSize	Size of the window for Go-Back_N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(int windowSize, int rtoTimer) {
		window = windowSize;
		delay = Long.valueOf(rtoTimer);

		que = new TxQueue(window);
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

		hostName = serverName;
		TCPServerPort = serverPort;
		fileName = fileN;

		File file = new File(fileName);

		if(!file.exists()){
			System.out.println(fileName + " does not exist!");
			System.exit(0);
		}

		this.fileSize = file.length();

		try{
			TCPSocket = new Socket(hostName, TCPServerPort);
			UDPSocket = new DatagramSocket();

			DataOutputStream outStream = new DataOutputStream(TCPSocket.getOutputStream());
			DataInputStream inpStream = new DataInputStream(TCPSocket.getInputStream());

			outStream.writeUTF(fileName);
			outStream.writeLong(fileSize);
			outStream.writeInt(UDPSocket.getLocalPort());
			outStream.flush();

			UDPServerPort = inpStream.readInt();
			System.out.println("TCP Server: " + serverName + ", Port: " + serverPort + "\n");
			System.out.println("UDP Server: " + serverName + ", Port: " + UDPServerPort + "\n");

			ReceiverThread receiverT = new ReceiverThread(UDPSocket, hostName, UDPServerPort, que, delay);
			SenderThread senderT = new SenderThread(UDPSocket, hostName, UDPServerPort, que, delay, fileName);

			senderT.setReceiver(receiverT);
			receiverT.setSender(senderT);

			Thread sender = new Thread(senderT);
			Thread receiver = new Thread(receiverT);

			sender.start();
			receiver.start();

			sender.join();
			receiver.join();

			TCPSocket.close();
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}catch(InterruptedException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
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
		System.out.println("\nfile transfer completed.");
	}
}
