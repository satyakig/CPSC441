/**
 * @author Satyaki Ghosh
 *         Nov 10 2017
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;

public class ReceiverThread implements Runnable {
    private SenderThread senderT;

    private TxQueue que;

    private Timer timer;
    private long delay;

    private DatagramSocket UDPSocket;
    private InetAddress serverAddress;
    private int serverPort;

    private volatile boolean eof;

    public ReceiverThread(DatagramSocket soc, String server, int p, TxQueue queue, long d) {
        try {
            que = queue;

            timer = null;
            delay = d;

            UDPSocket = soc;
            serverPort = p;
            serverAddress = InetAddress.getByName(server);

            eof = false;
        }catch(UnknownHostException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true) {
            try {
                byte[] data = new byte[4];
                DatagramPacket packet = new DatagramPacket(data, data.length);

                UDPSocket.receive(packet);
                this.processACK(new Segment(data));

                if(eof && que.isEmpty()) {
                    UDPSocket.close();
                    break;
                }
                Thread.sleep(750);
            }catch(IOException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }catch(InterruptedException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public synchronized void processACK(Segment seg) throws InterruptedException{

        System.out.println("received ack# " + seg.getSeqNum());

        if(que.element().getSeqNum() <= seg.getSeqNum()) {
            if(timer != null)
                timer.cancel();

            while(que.element() != null && que.element().getSeqNum() < seg.getSeqNum())
                que.remove();

            if(!que.isEmpty())
                setTimer();
        }
    }

    public void fileRead() {
        eof = true;
    }

    public void setSender(SenderThread t) {
        senderT = t;
    }

    private void setTimer() {
        if(timer != null)
            timer.cancel();

        timer = new Timer(true);
        timer.schedule(new TimeoutHandler(que, UDPSocket, serverAddress, serverPort), delay);
        senderT.setExternalTimer(timer);
    }

    public void setExternalTimer(Timer t) {
        this.timer = t;
    }


}
