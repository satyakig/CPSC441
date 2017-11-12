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
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Files;

public class SenderThread implements Runnable {
    private ReceiverThread receiverT;

    private TxQueue que;

    private Timer timer;
    private long delay;

    private File file;
    private byte[] data;
    private int[] splits;
    private int dataIndex;

    private DatagramSocket UDPSocket;
    private InetAddress serverAddress;
    private int serverPort;

    public final static int MAX_PAYLOAD_SIZE = 1000;


    public SenderThread(DatagramSocket soc, String server, int p, TxQueue queue, long del, String f) {
        try {
            que = queue;

            timer = null;
            delay = del;

            UDPSocket = soc;
            serverPort = p;
            serverAddress = InetAddress.getByName(server);

            file = new File(f);
            data = Files.readAllBytes(Paths.get(file.getPath()));
            dataIndex = 0;

            int quo = data.length / MAX_PAYLOAD_SIZE;
            int rem = data.length % MAX_PAYLOAD_SIZE;

            if(rem != 0 && quo != 0)
                splits = new int[quo + 1];
            else if(rem != 0 && quo == 0)
                splits = new int[1];
            else
                splits = new int[quo];

            if(rem != 0 && quo != 0) {
                int i;
                for(i = 0; i < splits.length - 1; i++) {
                    splits[i] = (i + 1) * 1000;
                }
                splits[i] = i * 1000 + rem;
            }
            else if(rem != 0 && quo == 0)
                splits[0] = rem;
            else {
                for(int i = 0; i < splits.length; i++)
                    splits[i] = (i + 1) * 1000;
            }

        }catch(UnknownHostException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            for(int i = 0; i < splits.length; i++) {
                byte[] payload;
                if(i == 0)
                    payload = new byte[splits[i]];
                else
                    payload = new byte[splits[i] - splits[i - 1]];

                for(int j = 0; j < payload.length; j++, dataIndex++)
                    payload[j] = data[dataIndex];

                Segment s = new Segment(i, payload);

                while(que.isFull())
                    Thread.sleep(500);

                this.processSend(s);
            }

            receiverT.fileRead();

        }catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void processSend(Segment seg) {

        try {
            System.out.println("sending seq# " + seg.getSeqNum());

            DatagramPacket packet = new DatagramPacket(seg.getBytes(), seg.getBytes().length, serverAddress, serverPort);
            UDPSocket.send(packet);
            que.add(seg);

            if(que.size() == 1)
                setTimer();

        }catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }catch(InterruptedException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void setReceiver(ReceiverThread t) {
        receiverT = t;
    }

    private void setTimer() {
        if(timer != null)
            timer.cancel();

        timer = new Timer(true);
        timer.schedule(new TimeoutHandler(que, UDPSocket, serverAddress, serverPort), delay);
        receiverT.setExternalTimer(timer);
    }

    public void setExternalTimer(Timer t) {
        this.timer = t;
    }
}
