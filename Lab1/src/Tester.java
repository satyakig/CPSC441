/**
 * A simple test driver
 *
 * @author 	Majid Ghaderi
 * @version	3.2, Sep 22, 2017
 *
 */

import java.io.IOException;

public class Tester {

	public static void main(String[] args) {

		String[] url = {"people.ucalgary.ca/~mghaderi/index.html",
				"people.ucalgary.ca/~mghaderi/test/uc.gif",
				"people.ucalgary.ca/~mghaderi/test/a.pdf",
				"people.ucalgary.ca:80/~mghaderi/test/test.html"};

		String[] url2 = {
				"GET TCPServer.java HTTP/1.0",
				"GET /TCPServer.java HTTP/1.0",
				"GET cpsc441-tcp-server.pdf HTTP/1.0",
				"GET /cpsc441-threading.pdf HTTP/1.0",
				"GET /hello/gyph.gif HTTP/1.1",
				"GET hello/hello2/index.html HTTP/0.9",
				"GET /hello/hello2/sup/a.pdf HTTP/2.0",
				"GET /TaCPServer.java HTTP/1.11",
				"GET /hello/gy2ph.gif HTTP/1.21",
				"GET /hello/hello2/ind1ex.html HTTP/.1",
				"GET /hello/hello2/sup/a1.pdf HTTP/2.1",
				"GET /hello/hel2lo2/sup/a.pdf HTTP/.1",
				"GET /hel1lo/hel2lo2/sup/a.pdf HTTP/1.1",
				};

		String[] url3 = {
				"localhost:2225/TCPServer.java",
				"localhost:2225/hello.txt",
				"localhost:2225/cpsc441-threading.pdf",
				"localhost:2225/hello/gyph.gif",
				"localhost:2225/hello/hello2/index.html",
				"localhost:2225/hello/hello2/sup/a.pdf",
		};

		try {
			UrlCache cache = new UrlCache();

//			cache.getObject(url3[1]);
//			for(int i = 0; i < url3.length; i++)
//				cache.getObject(url3[i]);

//			for(int i = 0; i < url2.length; i++)
//				cache.getObject2(url2[i]);

//			for (int i = 0; i < url.length; i++)
//				cache.getObject(url[i]);
//
//			System.out.println("Last-Modified for " + url[0] + " is: " + cache.getLastModified(url[0]));
//			cache.getObject(url[0]);
//			System.out.println("Last-Modified for " + url[0] + " is: " + cache.getLastModified(url[0]));
//
//			cache.printCatalog();
		}
		catch (IOException e) {
			System.out.println("There was a problem: " + e.getMessage());
		}
	}
}
