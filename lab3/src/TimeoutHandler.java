/**
 * @author Satyaki Ghosh
 *         Nov 9 2017
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;

public class TimeoutHandler extends TimerTask {

    private DatagramSocket UDPSocket;
    private InetAddress serverAddress;
    private int serverPort;

    private TxQueue que;

    public TimeoutHandler(TxQueue q, DatagramSocket soc, InetAddress add, int p) {

        que = q;

        UDPSocket = soc;
        serverAddress = add;
        serverPort = p;
    }

    @Override
    public void run() {
        try {

            Segment[] segments = que.toArray();
            for(int i = 0; i < segments.length; i++) {
                Segment seg = segments[i];
                System.out.println("timeout sending seq# " + seg.getSeqNum());

                DatagramPacket packet = new DatagramPacket(seg.getBytes(), seg.getBytes().length, serverAddress, serverPort);
                UDPSocket.send(packet);
            }

        }catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
