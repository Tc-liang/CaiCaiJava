package _11并发;

import java.util.concurrent.TimeUnit;

/**
 * @author: cl
 * @create: 2024/4/8 10:28
 * @description:
 */
public class A同步访问可变共享数据 {
    private static  boolean stopRequested;
//    private static volatile boolean stopRequested;

    public static void main(String[] args) throws InterruptedException {
        method();
    }

    /**
     * 错误用法 永远循环 除非使用volatile
     *
     * @throws InterruptedException
     */
    private static void method() throws InterruptedException {
        Thread backgroundThread = new Thread(() -> {
            int i = 0;
            while (!stopRequested)
                i++;
            System.out.println(i);
        });
        backgroundThread.start();

        TimeUnit.SECONDS.sleep(1);
        stopRequested = true;
    }


    /**
     * 加本地同步锁
     *
     * @throws InterruptedException
     */
    private static void methodSync() throws InterruptedException {
        Thread backgroundThread = new Thread(() -> {
            int i = 0;
            while (!stopRequested())
                i++;
            System.out.println(i);
        });
        backgroundThread.start();

        TimeUnit.SECONDS.sleep(1);
        requestStop();
    }

    private static synchronized void requestStop() {
        stopRequested = true;
    }

    private static synchronized boolean stopRequested() {
        return stopRequested;
    }


}
