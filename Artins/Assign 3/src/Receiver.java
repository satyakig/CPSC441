import java.io.IOException;
import java.net.DatagramPacket;

public class Receiver extends Thread {
	
	private FastFtp ftp;
	
	public Receiver(FastFtp ff){
		this.ftp = ff;
	}
	
	public void run(){
		while(true){
// Byte array to receive data into
			byte[] receiveData = new byte[Segment.MAX_PAYLOAD_SIZE];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
// Receiving a datagram from server
			try {
				ftp.clientSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
// Getting a segment from the datagram 
			Segment acksegment = new Segment(receiveData);
			System.out.println("receiver seq number:" +acksegment.getSeqNum());
			
// Calling processAck to process the ack receiving
			this.ftp.processAck(acksegment);
			
// If all the acks have been processed, notify the sender thread
			TxQueue transmissionQ = this.ftp.getTxqueue();
			if(this.ftp.getEOF() == true && transmissionQ.isEmpty()){
				return;
			}
		}
	}
}
