import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TimerTask;

import cpsc441.a3.TxQueue;

public class TimeOutHandler extends TimerTask {
	
	private FastFtp ftp;

// Basic constructor
	public TimeOutHandler(FastFtp ff){
		this.ftp = ff;
	}

// Run method that handles the timeout via calling processTime
	public void run(){
		this.ftp.processTime();
	}
		
	
}
