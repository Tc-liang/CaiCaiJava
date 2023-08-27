package B_synchronized;

import java.io.IOException;

/**
 * @Author: Caicai
 * @Date: 2023-08-27 21:56
 * @Description: object monitor 阻塞队列
 */
public class C_SynchronizedHeavyWeightLock {
    public static void main(String[] args) throws Throwable {
        Object obj = new Object();
        new Thread(() -> {
            synchronized (obj) {
                try {
                    //输入阻塞
                    //阻塞的目的是让  其他线程自旋完未获取到锁，进入cxq栈
                    System.in.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + " 获取到锁");
            }
        }, "t1").start();

        //sleep控制线程阻塞的顺序
        Thread.sleep(50);
        new Thread(() -> {
            synchronized (obj) {
                System.out.println(Thread.currentThread().getName() + " 获取到锁");
            }
        }, "t2").start();

        Thread.sleep(50);
        new Thread(() -> {
            synchronized (obj) {
                System.out.println(Thread.currentThread().getName() + " 获取到锁");
            }
        }, "t3").start();

        Thread.sleep(50);
        new Thread(() -> {
            synchronized (obj) {
                System.out.println(Thread.currentThread().getName() + " 获取到锁");
            }
        }, "t4").start();

        Thread.sleep(50);
        new Thread(() -> {
            synchronized (obj) {
                System.out.println(Thread.currentThread().getName() + " 获取到锁");
            }
        }, "t5").start();

        Thread.sleep(50);
        new Thread(() -> {
            synchronized (obj) {
                System.out.println(Thread.currentThread().getName() + " 获取到锁");
            }
        }, "t6").start();

        //cxq: t2,t3,t4,t5,t6
        //t1释放锁前 将cxq的节点出栈加入entry list:t6,t5,t4,t3,t2 并唤醒队头节点
    }
}
