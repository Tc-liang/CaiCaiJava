package A_Thread;

import java.util.concurrent.TimeUnit;

/**
 * @author Tc.l
 * @Date 2020/12/2
 * @Description:
 * 测试终止线程
 */
public class InterruptThread {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()){
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();

                    //终止自己 退出循环
                    Thread.currentThread().interrupt();
                }
                System.out.println("==================");
            }
        });

        thread.start();
        System.out.println(thread.isInterrupted());
        thread.interrupt();
        System.out.println(thread.isInterrupted());
    }
}
