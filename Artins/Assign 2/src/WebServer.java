import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WebServer  extends Thread {

	private volatile boolean shutdown = false;
	private ServerSocket serverSocket;

// basic constructor
	WebServer(int port) {
		try {
			this.serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

// Webserver thread that takes the serversocket and creates a worker thread to execute all the logic
	public void run(){
		while(shutdown != true){
			workerThread wk;
			try {
				Socket socket = this.serverSocket.accept();
				wk = new workerThread(socket);
				wk.start();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		try {
			this.serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
// Shutdown the web server 
	public void shutdown(){
		shutdown = true;

	}

	
}
