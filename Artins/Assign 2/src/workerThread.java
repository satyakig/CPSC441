import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class workerThread extends Thread {
	
	private Socket connection;
	private String command;
	private String status;
	private String response = " ";
	private String path;
	private String parts[];
	private byte[] http_header_in_bytes;
	private byte[] read;
// Basic constructor 
	workerThread(Socket s){

		this.connection = s;
	}
	
	public void run(){
			String in;
			
// Get the HTTP request
			try
			 {
				 OutputStream outputStream = this.connection.getOutputStream();
				 //PrintWriter outputStream = new PrintWriter(new
				// OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
				 Scanner inputStream = new Scanner(connection.getInputStream(), "UTF-8"); 

				 while(true){
					 in = inputStream.nextLine();
					 System.out.println(in);
					 if(in.equalsIgnoreCase("bye")){
						 ((PrintStream) outputStream).println("bye");
						 outputStream.flush();
						 break;
					 }
					 
// Parse the request and get the command
					 parts = in.split(" ");
					 command = parts[0].trim();
					 String temp[] = parts[1].split("/");
					 path = temp[1].trim();
					 status = parts[2].trim();
					 System.out.println(path);
					 
					 File obj = new File(path);
					 
// Determine if the command is valid and take appropriate measures upon the requests validation
					 
					 if(command.equalsIgnoreCase("get")){

// Determine if the requested object exists on the local host. Send 200 OK as response if true otherwise 404 Not found.
						 if(obj.exists()){
							 response = status + " " + "200 OK\r\n";
						 }
						 
						 else{
							 response = status + " " + "404 Not Found\r\n\r\n";
							 http_header_in_bytes = response.getBytes("US-ASCII");
							 outputStream.write(http_header_in_bytes);
							 outputStream.flush();
							 break;
						 }
 
					 }
					 else{
						 response = status + " " + "400 Bad Request\r\n\r\n";
						 http_header_in_bytes = response.getBytes("US-ASCII");
						 outputStream.write(http_header_in_bytes);
						 outputStream.flush();
						 break;
					 }
					 
// Get the length of the object
					 int len = (int) obj.length();
					 
					 response.concat("Conten length: " + Integer.toString(len) +"\r\n");
					 response.concat("Connection: Close\r\n\r\n");
					 
// Send the header file over the current connection
					 http_header_in_bytes = response.getBytes("US-ASCII");
					 outputStream.write(http_header_in_bytes);
					 
// Read the file and send it over the current connection	 
					 read = new byte[len];
					 FileInputStream input = new FileInputStream(obj);
					 int num_bytes = 0;
					 
					num_bytes = input.read(read);
					while( num_bytes!=-1){
						
						outputStream.write(read);
						num_bytes = input.read(read);
					 }
					  input.close();
					 ((PrintStream) outputStream).println(in);
					 outputStream.flush(); 
				 }
	}
			catch(Exception e){
				
			}
			finally{
				if(connection != null){
					try {
						connection.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
			}
	}
}
