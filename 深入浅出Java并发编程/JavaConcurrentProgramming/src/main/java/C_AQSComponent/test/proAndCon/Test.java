package C_AQSComponent.test.proAndCon;

import C_AQSComponent.A_MySynchronizedComponent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author: Caicai
 * @Date: 2023-09-02 19:19
 * @Description:
 */
public class Test {
    public static void main(String[] args) {
        Lock lock = new A_MySynchronizedComponent();
        Condition proCondition = lock.newCondition();
        Condition conCondition = lock.newCondition();
        Num num = new Num();

        Product p1 = new Product(lock, proCondition, conCondition);
        Product p2 = new Product(lock, proCondition, conCondition);
        Consumer c1 = new Consumer(lock, proCondition, conCondition);
        Consumer c2 = new Consumer(lock, proCondition, conCondition);


        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                p1.pro(num);
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                p2.pro(num);
            }
        });
        Thread t3 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                c1.con(num);
            }
        });
        Thread t4 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                c2.con(num);
            }
        });

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        try {
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
