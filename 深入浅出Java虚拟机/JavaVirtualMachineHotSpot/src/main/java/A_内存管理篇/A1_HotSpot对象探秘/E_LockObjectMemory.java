package A_内存管理篇.A1_HotSpot对象探秘;

import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;

/**
 * @Author: Caicai
 * @Date: 2023-07-29 9:06
 * @Description: 加锁时的对象头空间
 * <p>
 * lock 01无锁 00轻量级锁 10重量级锁
 * |----------------------------------------------------------------------|--------|------------------------------|
 * | unused:25 | identity_hashcode:31 | unused:1 | age:4 | biased_lock:1  | lock:2 |     OOP to metadata object   |  无锁
 * |----------------------------------------------------------------------|--------|------------------------------|
 * |  thread:54 |         epoch:2      | unused:1 | age:4 | biased_lock:1 | lock:2 |     OOP to metadata object   |  偏向锁
 * |----------------------------------------------------------------------|--------|------------------------------|
 * |                     ptr_to_lock_record:62                            | lock:2 |     OOP to metadata object   |  轻量锁
 * |----------------------------------------------------------------------|--------|------------------------------|
 * |                     ptr_to_heavyweight_monitor:62                    | lock:2 |     OOP to metadata object   |  重量锁
 */
public class E_LockObjectMemory {


    /**
     * 测试无锁状态下的mark word 无锁锁标记为01(1)
     */
    @Test
    public void noLock() {
        Object obj = new Object();
        //mark word  0 0000 0 01 被unused:1，age:4，biased_lock:1，lock:2使用，并且锁状态为01表示无锁
        //01 00 00 00  (00000001 00000000 00000000 00000000)
        //00 00 00 00  (00000000 00000000 00000000 00000000)
        ClassLayout objClassLayout = ClassLayout.parseInstance(obj);
        System.out.println(objClassLayout.toPrintable());

        //计算一致性哈希后
        //01 b6 ce a8
        //6a 00 00 00
        obj.hashCode();
        System.out.println(objClassLayout.toPrintable());

        //进行GC 查看GC年龄 0 0001 0 01 前2位表示锁状态01无锁，第三位biased_lock为0表示未启用偏向锁，后续四位则是GC年龄age 1
        //09 b6 ce a8 (00001001 10110110 11001110 10101000)
        //6a 00 00 00 (01101010 00000000 00000000 00000000)
        System.gc();
        System.out.println(objClassLayout.toPrintable());

    }

    /**
     * 测试偏向锁
     * 充分时间让偏向锁启动
     * 匿名偏向锁 还未设置偏向线程
     * 有竞争膨胀为轻量级锁
     *
     * @throws InterruptedException
     */
    @Test
    public void biasedLockTest() throws InterruptedException {
        //延迟让偏向锁启动
        Thread.sleep(5000);
        Object obj = new Object();
        ClassLayout objClassLayout = ClassLayout.parseInstance(obj);

        //1 01 匿名偏向锁 还未设置偏向线程
        //05 00 00 00 (00000101 00000000 00000000 00000000)
        //00 00 00 00 (00000000 00000000 00000000 00000000)
        System.out.println(Thread.currentThread().getName() + ":");
        System.out.println(objClassLayout.toPrintable());

        synchronized (obj) {
            //偏向锁 记录 线程地址
            //05 30 e3 02 (00000101 00110000 11100011 00000010)
            //00 00 00 00 (00000000 00000000 00000000 00000000)
            System.out.println(Thread.currentThread().getName() + ":");
            System.out.println(objClassLayout.toPrintable());
        }

        Thread thread1 = new Thread(() -> {
            synchronized (obj) {
                //膨胀为轻量级
                //68 f4 a8 1d (01101000 11110100 10101000 00011101)
                //00 00 00 00 (00000000 00000000 00000000 00000000)
                System.out.println(Thread.currentThread().getName() + ":");
                System.out.println(objClassLayout.toPrintable());
            }
        }, "t1");

        thread1.start();
        thread1.join();
    }

    /**
     * 测试轻量级锁 重量级锁标记为00(0)
     * 获取到轻量级锁后 hashcode存储在拥有锁的线程中
     * 轻量级锁 释放后 会退化到无锁状态
     *
     * @throws InterruptedException
     */
    @Test
    public void lightLockTest() throws InterruptedException {
        Object obj = new Object();
        ClassLayout objClassLayout = ClassLayout.parseInstance(obj);
        //1334729950
        System.out.println(obj.hashCode());
        //01 4e c0 d5 (00000001 01001110 11000000 11010101)
        //6a 00 00 00 (01101010 00000000 00000000 00000000)
        System.out.println(Thread.currentThread().getName() + ":");
        System.out.println(objClassLayout.toPrintable());


        Thread thread1 = new Thread(() -> {
            synchronized (obj) {
                // 110110 00 中的00表示轻量级锁其他62位指向拥有锁的线程
                //d8 f1 5f 1d (11011000 11110001 01011111 00011101)
                //00 00 00 00 (00000000 00000000 00000000 00000000)
                System.out.println(Thread.currentThread().getName() + ":");
                System.out.println(objClassLayout.toPrintable());

                //1334729950
                //无锁升级成轻量级锁后 hashcode未变 对象头中没存储hashcode 只存储拥有锁的线程
                //（实际上mark word内容被存储到线程中，所以hashcode也被存储到线程中）
                System.out.println(obj.hashCode());
            }
        }, "t1");

        thread1.start();
        //等待t1执行完 避免 发生竞争
        thread1.join();

        //轻量级锁 释放后 mark word 恢复成无锁 存储哈希code的状态
        //01 4e c0 d5 (00000001 01001110 11000000 11010101)
        //6a 00 00 00 (01101010 00000000 00000000 00000000)
        System.out.println(Thread.currentThread().getName() + ":");
        System.out.println(objClassLayout.toPrintable());

        Thread thread2 = new Thread(() -> {
            synchronized (obj) {
                //001010 00 中的00表示轻量级锁其他62位指向拥有锁的线程
                //28 f6 5f 1d (00101000 11110110 01011111 00011101)
                //00 00 00 00 (00000000 00000000 00000000 00000000)
                System.out.println(Thread.currentThread().getName() + ":");
                System.out.println(objClassLayout.toPrintable());
            }
        }, "t2");
        thread2.start();
        thread2.join();
    }


    /**
     * 测试重量级锁 重量级锁标记为10(2)
     * 获取到重量级锁后，hashcode存储在监视器对象
     * 膨胀为重量级锁后，一般不会锁降级
     */
    @Test
    public void heavyLockTest() throws InterruptedException {
        Object obj = new Object();
        ClassLayout objClassLayout = ClassLayout.parseInstance(obj);
        Thread thread1 = new Thread(() -> {
            synchronized (obj) {
                //第一次 00 表示 轻量级锁
                //d8 f1 c3 1e (11011000 11110001 11000011 00011110)
                //00 00 00 00 (00000000 00000000 00000000 00000000)
                System.out.println(Thread.currentThread().getName() + ":");
                System.out.println(objClassLayout.toPrintable());

                //第二次打印 变成 10 表示膨胀为重量级锁（t2竞争）  其他62位指向监视器对象
                //fa 21 3e 1a (11111010 00100001 00111110 00011010)
                //00 00 00 00 (00000000 00000000 00000000 00000000)
                System.out.println(Thread.currentThread().getName() + ":");
                System.out.println(objClassLayout.toPrintable());
            }
        }, "t1");

        thread1.start();

        Thread thread2 = new Thread(() -> {
            synchronized (obj) {
                //t2竞争 膨胀为 重量级锁 111110 10 10为重量级锁
                //fa 21 3e 1a (11111010 00100001 00111110 00011010)
                //00 00 00 00 (00000000 00000000 00000000 00000000)
                System.out.println(Thread.currentThread().getName() + ":");
                System.out.println(objClassLayout.toPrintable());
            }
        }, "t2");
        thread2.start();

        thread1.join();
        thread2.join();

        //10 重量级锁 未发生锁降级
        //3a 36 4d 1a (00111010 00110110 01001101 00011010)
        //00 00 00 00 (00000000 00000000 00000000 00000000)
        System.out.println(Thread.currentThread().getName() + ":");
        System.out.println(objClassLayout.toPrintable());
    }
}
