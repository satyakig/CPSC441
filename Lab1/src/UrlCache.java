/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 1
 * T-01
 */

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;
import java.net.Socket;
import java.io.*;

public class UrlCache {

	private HashMap<String, Long> catalog;
	private Socket socket;
	private ObjectInputStream objectIn;
	private ObjectOutputStream objectOut;

	/**
	 * Default constructor to initialize the HashMap data structure - catalog, that is used for caching
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 * Looks for a serialized file called "catalog.ser" to load the catalog from
	 * if "catalog.ser" doesn't exist, creates a empty HashMap called catalog
	 * @throws IOException if encounters any errors/exceptions
	 */
	public UrlCache() throws IOException {
		catalog = new HashMap<String, Long>();
		socket = null;
		objectIn = null;
		objectOut = null;

		File file = new File("catalog.ser");

		if(file.exists() && !file.isDirectory()) {
			objectIn = new ObjectInputStream(new FileInputStream("catalog.ser"));
			try{
				catalog = (HashMap<String, Long>) objectIn.readObject();
			}catch(EOFException e) {
			}catch(ClassNotFoundException e) {
				System.out.println(e.getMessage());
			}

			objectIn.close();
			if(catalog.size() > 0)
				System.out.println("Catalog loaded from cache.");
			else
				System.out.println("Error reading catalog from cache.");
		}
		else
			System.out.println("There is no catalog in cache, starting a new catalog.");
		System.out.println();
	}

	/**
	 * Downloads the object specified by the parameter url if the local copy is out of date.
	 * @param url	URL of the object to be downloaded. It is a fully qualified URL.
	 * @returns nothing
	 * @throws IOException if encounters any errors/exceptions
	 */
	public void getObject(String url) throws IOException {
		//default port 80
		int port = 80;
		PrintWriter outStream = null;
		InputStream inStream = null;
		OutputStream print = null;

		//hostname for the object to be downloaded
		String host = "";
		//object name/path to be downloaded
		String obj = "";

		//parses the parameter url to find the hostname, object path/name and port# if it exists
		String[] words = url.split("/");
		for(int i = 0; i < words.length; i++) {
			if(i == 0){
				if(words[i].indexOf(':') != -1) {
					port = Integer.parseInt(words[i].split(":")[1]);
					host = words[i].split(":")[0];
				}
				else
					host = words[i];
			}
			else
				obj += ("/" + words[i]);
		}

		//creates a filename from the url that is going to be used to save the file locally
		char[] path = obj.substring(1, obj.length()).toCharArray();
		for(int i = 0; i < path.length; i++){
			char c = path[i];
			if(c == 34 || c == 42 || c == 47 || c == 58 || c == 60 || c == 62 || c == 63 || c == 92 || c == 124)
				path[i] = '.';
		}
		String filename = new String(path);

		//creates the TCP connection with the server and opens the input and output streams of the socket
		socket = new Socket(host, port);
		outStream = new PrintWriter(new DataOutputStream(socket.getOutputStream()));
		inStream = socket.getInputStream();

		byte[] bytes = new byte[16*1024];
		int count;

		//sends the GET request to the server
		outStream.print("GET " + obj + " HTTP/1.1\r\nHost: " + host + "\r\n\r\n");
		outStream.flush();

		//receives the response from the server, checks if the file specified exists locally and if it is up to date
		//saves the response body in a file locally ONLY if the local file is NOT up to date or does not exist
		for(int i = 0; (count = inStream.read(bytes)) > 0; i++){
			if(i == 0){
				String line = new String(bytes);
				String[] headers = line.split("\r\n");

				int j = 0;
				for(; !headers[j].contains("Last-Modified: "); j++){}

				String mod = headers[j].split("Last-Modified: ")[1];

				if(!catalog.containsKey(url) || convertLong(mod) > catalog.get(url)) {
					if(!catalog.containsKey(url))
						System.out.println(obj + " does not exist in cache, retrieving and saving file...");
					else
						System.out.println(obj + " is outdated, retrieving and saving the latest version...");
					catalog.put(url, convertLong(mod));
					print = new FileOutputStream(filename);
				}
				else {
					System.out.println(obj + " exists in cache and is up to date.");
					break;
				}
			}
			else
				print.write(bytes, 0, count);
		}
		System.out.println();

		//updates the catalog if needed
		this.writeCatalogToFile();

		//closes all the input and output streams and connections
		if(print != null)
			print.close();
		if(inStream != null)
			inStream.close();
		if(outStream != null)
			outStream.close();
		socket.close();
	}

	/**
	 * Returns the Last-Modified time associated with the object specified by the parameter url.
	 * @param url 	URL of the object
	 * @return the Last-Modified time in millisecond as in Date.getTime()
	 * @throws RuntimeException if the object specified by the url does not exist in the catalog/cache/locally
	 */
	public long getLastModified(String url) {
		if(catalog.containsKey(url)) {
			return catalog.get(url);
		}
		else
			throw new RuntimeException(url + " does not exist in cache!");
	}

	/**
	 * writes the current catalog to a serialized file "catalog.ser" to keep the file up to date
	 * @throws IOException if encounters any errors/exceptions
	 */
	private void writeCatalogToFile() {
		try {
			objectOut = new ObjectOutputStream(new FileOutputStream("catalog.ser"));
			objectOut.writeObject(catalog);
			objectOut.close();
		}catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Prints all the urls and their last-modified dates that are saved locally/cache to the terminal
	 */
	public void printCatalog() {
		System.out.println();
		for(String key: catalog.keySet()) {
			System.out.println("URL: " + key + ", Last-Modified: " + catalog.get(key));
		}
	}

	/**
	 * Calculates the Unix Time or Date.getTime() in milliseconds given a date in string format
	 * @param date	in the format EEE, dd MMM yyyy hh:mm:ss zzz
	 * @returns date in long integer format i.e. unix time
	 */
	public static long convertLong(String date) {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss zzz");
		Date dt = format.parse(date, new ParsePosition(0));
		return dt.getTime();
	}
}
