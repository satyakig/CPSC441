import java.net.DatagramSocket;
import java.util.Timer;

/**
 * @author Satyaki Ghosh
 *         Nov 10 2017
 */


public class SenderThread implements Runnable {

    private TxQueue que;
    private Timer timer;

    private DatagramSocket UDPSocket;
    private String serverName;
    private int serverPort;


    public SenderThread(DatagramSocket soc, TxQueue queue, String server, int p, Timer tim) {

        this.que = queue;
        this.timer = tim;

        this.UDPSocket = soc;
        this.serverName = server;
        this.serverPort = p;
    }


    @Override
    public void run() {

    }
}
