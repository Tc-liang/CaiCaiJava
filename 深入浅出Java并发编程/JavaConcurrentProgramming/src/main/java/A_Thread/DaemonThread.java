package A_Thread;

import java.util.concurrent.TimeUnit;

/**
 * @author Tc.l
 * @Date 2020/12/1
 * @Description:
 * 测试Daemon线程是否能执行
 */
public class DaemonThread {
    public static void main(String[] args) {
        Thread daemonThread = new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("DaemonThread的finally块被执行");
            }
        });
        daemonThread.setDaemon(true);
        daemonThread.start();

    }
}
