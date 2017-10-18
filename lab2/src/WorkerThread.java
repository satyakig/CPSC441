/**
 * @author Satyaki Ghosh
 * CPSC 441
 * Assignment 2
 * T-01
 */

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class WorkerThread extends Thread {

    private Socket socket;
    private OutputStream output;
    private Scanner input;

    private String request;

    // basic constructor for worker thread
    // initializes with the socket
    // creates the input and out streams for reading and writing to the socket
    public WorkerThread(Socket soc) {
        request = "";

        try{
            socket = soc;
            input = new Scanner(new InputStreamReader(socket.getInputStream()));
            output = socket.getOutputStream();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }

    }

    // start method for Thread
    // only accepts one request from the client and sends back the appropriate response
    // closes the connection with client after sending the response
    @Override
    public void run() {
        try {
            System.out.println();

            // only reads one request from client
            int count = 0;
            while(input.hasNextLine()){
                request = input.nextLine();
                if(count == 0) break;
            }

            // parses the request and gets its parameters
            String[] requests = request.split(" ");
            for(int i = 0; i < requests.length; i++)
                requests[i] = requests[i].trim();

            // if the request doesn't have the correct number of parameter, returns Bad Request
            if(requests.length != 3){
                output.write(this.getBadResponse(400).getBytes("US-ASCII"));
                output.flush();
                System.out.println("400 Bad Request: " + request);
            }
            // if the request doesn't use the GET or get parameter, returns Bad Request
            else if(!(requests[0].equals("get") || requests[0].equals("GET"))){
                output.write(this.getBadResponse(400).getBytes("US-ASCII"));
                output.flush();
                System.out.println("400 Bad Request: " + request);
            }
            // if the request doesn't use the correct HTTP protocol, returns Bad Request
            else if(!(requests[2].equals("HTTP/1.1") || requests[2].equals("HTTP/1.0") || requests[2].equals("HTTP/2.0") || requests[2].equals("HTTP/0.9"))) {
                output.write(this.getBadResponse(400).getBytes("US-ASCII"));
                output.flush();
                System.out.println("400 Bad Request: " + request);
            }
            // request has the correct parameters and protocols
            else {
                String path = requests[1];
                if(path.charAt(0) == '/')
                    path = path.substring(1);

                // gets the object pathname that the client requested
                File file = new File(path);

                // if the file doesn't exist on the server, or if the pathname specified is a directory
                // returns Not Found to the client
                if(!file.exists() || file.isDirectory()){
                    output.write(this.getBadResponse(404).getBytes("US-ASCII"));
                    output.flush();
                    System.out.println("404 Not Found: " + request);
                }
                // if the file exists on server and the pathname specified is a file
                // returns OK and the file to the client
                else if(file.exists() && file.isFile()){
                    this.sendGoodResponse(file);
                    System.out.println("200 OK: " + request);
                }
                // everything else returns a Bad Request
                else{
                    output.write(this.getBadResponse(400).getBytes("US-ASCII"));
                    output.flush();
                    System.out.println("404 Not Found: " + request);
                }
            }

            // closes all the streams and the socket
            output.close();
            input.close();
            socket.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    // if the request is invalid or if the requested object is not found
    // this method creates the response to be sent back to the client
    private String getBadResponse(int status){
        DateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz");
        Date date = new Date();
        String response = "";

        if(status == 400)
            response = "HTTP/1.1 400 Bad Request\r\n";
        else if(status == 404)
            response = "HTTP/1.1 404 Not Found\r\n";

        response += "Date: " + format.format(date) + "\r\n";
        response += "Server: Localhost, Port:2225\r\n";
        response += "Connection: close\r\n\r\n";

        return response;
    }

    //if the request is valid and the object is found, specified in @param file
    // this method creates and sends the response headers the file to the client
    private void sendGoodResponse(File file) throws IOException {
        DateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz");
        Date date = new Date();

        String response = "HTTP/1.1 200 OK\r\n";
        response += "Date: " + format.format(date) + "\r\n";
        response += "Server: Localhost, Port:2225\r\n";
        response += "Last-Modified: " + this.getLastModified(file.lastModified()) + "\r\n";
        response += "Content-Length: " + file.length() + "\r\n";
        response += "Content-Type: " + this.getContentType(file.toPath()) + "\r\n";
        response += "Connection: close\r\n\r\n";

        Path path = Paths.get(file.getPath());
        byte[] data = Files.readAllBytes(path);

        this.output.write(response.getBytes("US-ASCII"));
        this.output.write(data);
        this.output.flush();

        return;
    }


    //converts @param time to a string in the specific format
    private String getLastModified(long time){
        SimpleDateFormat format = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzz");
        Date d = new Date(time);
        return format.format(d);
    }

    //gets the type of the file with the path specified in @param path
    private String getContentType(Path path) throws IOException{
        String type = Files.probeContentType(path);
        if(type == null)
            type = "Cannot determine type";

        return type;
    }
}
