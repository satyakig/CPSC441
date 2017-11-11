import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

/**
 * @author Satyaki Ghosh
 *         Nov 10 2017
 */

public class QueueThread implements Runnable {

    private File file;
    private TxQueue que;

    private byte[] data;

    public QueueThread(TxQueue q, File f) {
        file = f;
        que = q;

        try {
            data = Files.readAllBytes(Paths.get(file.getPath()));
        }catch(IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    @Override
    public void run() {

    }
}
