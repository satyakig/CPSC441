import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * UrlCache Class
 * 
 * @author 	Majid Ghaderi
 * @version	1.1, Sep 30, 2016
 *
 */
public class UrlCache {

	HashMap<String, String> catalog;
	File saved_catalog = new File("C:/Users/Artin/workspace/CPSC 441 Assignemt 1/saved_catalog.txt");
	Socket socket;
	
	
	
	
    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw exception.
	 *
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public UrlCache() throws UrlCacheException {
		
		catalog = new HashMap<String, String>();
		
		// if there is a catalog file on the system, populate cache with the existing catalog 
		if(this.saved_catalog.exists()){
			
			try {
				BufferedReader in = new BufferedReader( new FileReader("C:/Users/Artin/workspace/CPSC 441 Assignemt 1/saved_catalog.txt"));

				 String line;
				 while ((line = in.readLine()) != null) {
					 String parts[] = line.split(" ",2);
					 catalog.put(parts[0], parts[1]);
				 }
				in.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}		     
		}
		// else create a catalog file to save the cached urls in
		else{
			try {
				saved_catalog.createNewFile();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}
	
	
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public void getObject(String url) throws UrlCacheException {
		String parts [];
		String hostname;
		String pathname;
		String port;
		String requestLine1;
		String requestLine2;
		String if_modified_since;
		String eol;
		String http_header = "";
		String http_response_header_string = null;
		OutputStream out;
		InputStream in;
		byte[] http_header_in_bytes;
		byte[] http_response_header_bytes = new byte [2048];
		byte[] http_data_bytes = new byte [10*1024];
		int num_bytes_read = 0;
		int count = 0;
		int off = 0;
		int objectSize = 0;
		File f;
		
		
		
		
// Split the URL into its different components
		if(url.contains(":")){
			parts = url.split(":");
			hostname = parts[0];
			String temp = parts[1];
			String t[] = temp.split("/", 2);
			port = t[0];
			pathname = "/" + t[1];
		}
		else{
			parts = url.split("/", 2);
			hostname = parts[0];
			pathname = "/" + parts[1];
			port = "80";
		}
		
		requestLine1 = "GET " + pathname +  " HTTP/1.1\r\n";
		requestLine2 = "Host: " + hostname + ":"+ port + "\r\n";
		
		eol = "\r\n";

// Create a TCP connection 
		try {
			this.socket = new Socket(hostname,Integer.parseInt(port));
		
		} catch (NumberFormatException | IOException e) {
			
			e.printStackTrace();
		}
		
		
		
// HTTP request and response 

			// Send HTTP request
			try {
				
				if(this.catalog.containsKey(url)){
					if_modified_since = this.catalog.get(url);
					http_header = requestLine1 + requestLine2 + "If-modified-since: " + if_modified_since + "\r\n" + eol;
				}
				else
					http_header = requestLine1+ requestLine2 + eol;
				
				http_header_in_bytes = http_header.getBytes("US-ASCII");
				out = this.socket.getOutputStream();
				out.write(http_header_in_bytes);
				out.flush();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			
			
			// Get HTTP response 
			while(num_bytes_read != -1){
				try {
					in = this.socket.getInputStream();
					num_bytes_read = in.read(http_response_header_bytes, off, 1);
					off++;
					http_response_header_string =  new String(http_response_header_bytes, 0, off,"US-ASCII");
					if(http_response_header_string.contains("\r\n\r\n"))
						break; 
				} catch (IOException e) {
					
					e.printStackTrace();
				}
				
			}

			
			if(http_response_header_string.contains("304 Not Modified")){
				System.err.println("File already exists in the local storage");
			}
			
			else if(http_response_header_string.contains("200 OK")){
				
			// Getting the Last-Modified segment of the header to put in to the catalog
				parts = http_response_header_string.split("Last-Modified: ", 2);
				String t[] = parts[1].split("\r\n",2);
				this.catalog.put(url, t[0]);
				
				// Writing the catalog to a file
				try{
					PrintWriter output = new PrintWriter(new FileOutputStream("C:/Users/Artin/workspace/CPSC 441 Assignemt 1/saved_catalog.txt", true));
				    output.println(url +"   " +t[0]);
				    output.flush();
				    output.close();
				}
				catch (IOException e1) {
				e1.printStackTrace();
			}
			
			
			// Getting the Content-Length of the data segment to read into the bytes array
				parts = t[1].split("Content-Length: ", 2);
				String temp[] = parts[1].split("\r\n", 2);
				objectSize = Integer.parseInt(temp[0]);
				
				
			// Creating a files and subfiles with hostname/pathname
				f = new File("C:/Users/Artin/workspace/CPSC 441 Assignemt 1/" + hostname+"/" + pathname);
				if(!f.exists()){
					f.mkdirs();	
				}
				else{
					System.err.println("Files already exist");
					System.err.println("The existing files will be over written");
				}
				
			// Reading from the data segment to the file 
				try{
					// Getting the name of the file from the provided URL
					int lastindex = url.lastIndexOf("/");
					String filename = url.substring(lastindex);
					FileOutputStream output = new FileOutputStream("C:/Users/Artin/workspace/CPSC 441 Assignemt 1/" + hostname+"/" + pathname +"/"+ filename);
					while(num_bytes_read!= -1){
						if(count == objectSize)
							break;
				
						num_bytes_read = socket.getInputStream().read(http_data_bytes);
						
			// Writing (Downloading) bytes in to a file
						output.write(http_data_bytes);
						count = num_bytes_read;
						output.flush();
					 }
					output.close();
				} catch (IOException e){
					// Error in downloading file
				  } 
				
			}
	}

		
		
	
	
	
	
	
	
	
	
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     * @throws UrlCacheException if the specified url is not in the cache, or there are other errors/exceptions
     */
	public long getLastModified(String url) throws UrlCacheException {
		
		if(!this.catalog.containsKey(url))
			throw new UrlCacheException("The specified url is not stored in the cache");
		else{
			SimpleDateFormat d = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzzz");
			
			try {
				Date time = d.parse(this.catalog.get(url).trim());
				return time.getTime();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

}
