/**
 * @author Satyaki Ghosh
 *         Nov 9 2017
 */

import java.util.TimerTask;

public class TimeoutHandler extends TimerTask {

    int a = 112345;
    public TimeoutHandler() {

    }

    @Override
    public void run() {
        System.out.println(a);
    }
}
