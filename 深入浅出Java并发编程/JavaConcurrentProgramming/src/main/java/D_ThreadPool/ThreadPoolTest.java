package D_ThreadPool;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTest {

    @org.junit.Test
    public void testRunnable() {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1));

//        threadPool.execute(() -> {
//            int i = 1;
//            int j = 0;
//            System.out.println(i / j);
//        });

        threadPool.execute(() -> {
            try {
                int i = 1;
                int j = 0;
                System.out.println(i / j);
            } catch (Exception e) {
                System.out.println(e);
            }
        });

        threadPool.shutdown();
    }

    @org.junit.Test
    public void testCallable() {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1));

        Future<Integer> future = threadPool.submit(() -> {
            int i = 1;
            int j = 0;
            return i / j;
        });

        try {
            Integer integer = future.get();
        } catch (Exception e) {
            System.out.println(e);
        }

        Future<?> f = threadPool.submit(() -> {
            try {
                int i = 1;
                int j = 0;
                return i / j;
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                return null;
            }
        });


        threadPool.shutdown();
    }

    @org.junit.Test
    public void testAfterExecutor() {
        ThreadPoolExecutor threadPool = new MyThreadPool(1, 1,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));

        Future<Integer> future = threadPool.submit(() -> {
            int i = 1;
            int j = 0;
            return i / j;
        });

        threadPool.shutdown();
    }

    @org.junit.Test
    public void testUncaughtException() {
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),new MyThreadPoolFactory("order"),new ThreadPoolExecutor.AbortPolicy());

        threadPool.execute(() -> {
            int i = 1;
            int j = 0;
            System.out.println(i / j);
        });
    }
}