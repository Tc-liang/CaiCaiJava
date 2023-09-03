package C_AQSComponent;

import java.util.concurrent.*;

/**
 * @Author: Caicai
 * @Date: 2023-09-03 19:13
 * @Description:
 */
public class E_CyclicBarrier {
    public static void main(String[] args) throws BrokenBarrierException, InterruptedException {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3, () -> {
            System.out.println("所有线程到达屏障后,优先执行构造规定的runnable");
        });

        Thread t1 = new Thread(() -> {
            //执行任务
            task(cyclicBarrier);
        }, "t1");

        Thread t2 = new Thread(() -> {
            //执行任务
            task(cyclicBarrier);
        }, "t2");

        Thread t3 = new Thread(() -> {
            //执行任务
            task(cyclicBarrier);
        }, "t3");

        t1.start();
        t2.start();
        t3.start();

    }

    private static void task(CyclicBarrier cyclicBarrier) {
        System.out.println(Thread.currentThread() + "执行任务...");

        try {
            TimeUnit.SECONDS.sleep(1);

            cyclicBarrier.await();
            System.out.println("所有线程都执行完, " + Thread.currentThread() + "走出屏障");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
