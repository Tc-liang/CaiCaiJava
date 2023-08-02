package A_内存管理篇.A1_HotSpot对象探秘;

import java.io.IOException;

/**
 * @Author: Caicai
 * @Date: 2023-08-01 17:17
 * @Description: 重量级锁加解锁顺序
 * 重量级锁
 * cxq 栈 存储竞争节点
 * entry list 队列 存储稳定竞争失败节点
 * wait set 队列 存储等待节点
 * <p>
 * wait方法 加入等待队列
 * notify方法 将等待队列 队头节点 加入 cxq
 * <p>
 * 释放锁前：如果entry list不为空 唤醒队头节点，为空则将cxq所有节点出栈加入
 */
public class G_HeavyLockSort {
    public static void main(String[] args) throws Throwable {

        Object obj = new Object();
        new Thread(() -> {
            synchronized (obj) {
                try {
                    //输入阻塞
                    System.in.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + " 获取到锁");
            }
        }, "t1").start();

        //阻塞的目的是让  线程自旋完未获取到锁，进入cxq栈
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
