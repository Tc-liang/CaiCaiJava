package C_AQSComponent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Caicai
 * @Date: 2023-09-03 18:58
 * @Description:
 */
public class D_CountDownLatchTest {
    public static void main(String[] args) throws InterruptedException {
        //初始化10
        CountDownLatch countDownLatch = new CountDownLatch(10);
        //固定线程池
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 1; i <= 10; i++) {
            final int index = i;
            executor.execute(() -> {
                System.out.println(Thread.currentThread() + "处理任务" + index);

                //执行任务...

                //数量-1
                countDownLatch.countDown();
            });
        }


        //计数量为0时才可以继续执行
        countDownLatch.await();
        System.out.println("处理完任务");

        executor.shutdown();
    }
}
