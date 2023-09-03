package C_AQSComponent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Caicai
 * @Date: 2023-09-03 14:18
 * @Description: 信号量测试
 * 同时只能允许N个线程使用
 */
public class C_SemaphoreTest {
    public static void main(String[] args) {
        //初始化信号量
        Semaphore semaphore = new Semaphore(2);

        //每次只有两个线程能够获取到信号量执行
        ExecutorService executor =  Executors.newFixedThreadPool(4);
        for (int i = 0; i < 10; i++) {
            executor.execute(()->{
                try {
                    semaphore.acquire();
                    System.out.println(Thread.currentThread().getName()+"获得资源");

                    //执行任务
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    System.out.println(Thread.currentThread().getName()+"释放资源======");
                    semaphore.release();
                }
            });
        }

        executor.shutdown();
    }
}
