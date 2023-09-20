package A_Thread;

import java.util.concurrent.TimeUnit;

/**
 * @author Tc.l
 * @Date 2020/12/3
 * @Description: 测试Thread.interrupted()清楚标记位
 */
public class Interrupt {
    public static void main(String[] args) {
        Thread waitThread = new Thread(() -> {
            while (true){
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread busyThread = new Thread(() -> {
            while (true){
                Thread.interrupted();
            }
        });

        busyThread.setDaemon(true);
        waitThread.setDaemon(true);

        waitThread.start();
        busyThread.start();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        waitThread.interrupt();
        busyThread.interrupt();

        System.out.println("waitThread->"+waitThread.isInterrupted());
        System.out.println("busyThread->"+busyThread.isInterrupted());
//        busyThread.join();
    }
}
