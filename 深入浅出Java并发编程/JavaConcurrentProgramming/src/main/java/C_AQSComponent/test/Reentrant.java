package C_AQSComponent.test;

import C_AQSComponent.A_MySynchronizedComponent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @Author: Caicai
 * @Date: 2023-09-02 19:21
 * @Description: 可重入
 */
public class Reentrant {
    public static void main(String[] args) throws InterruptedException {
        final Lock lock  = new A_MySynchronizedComponent();

        for (int i = 1; i <= 20; i++) {
            new Thread(()->{
                reentrant(lock);
                System.out.println();
            },"t"+i).start();
        }

        reentrant(lock);

        TimeUnit.SECONDS.sleep(2);
    }

    private static void reentrant(Lock lock) {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "获取锁");
            //重入
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "重入获取锁");
            } finally {
                lock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }
}
