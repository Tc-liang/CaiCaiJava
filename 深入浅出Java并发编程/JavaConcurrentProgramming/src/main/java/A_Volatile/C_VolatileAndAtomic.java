package A_Volatile;

/**
 * @Author: Caicai
 * @Date: 2023-08-22 22:44
 * @Description:volatile的原子性
 */
public class C_VolatileAndAtomic {

    private volatile int num = 0;

    /**
     * 测试volatile原子性
     * 两个线程循环自增一万次
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        C_VolatileAndAtomic test = new C_VolatileAndAtomic();
        Thread t1 = new Thread(() -> {
            forAdd(test);
        });

        Thread t2 = new Thread(() -> {
            forAdd(test);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        //13710
        System.out.println(test.num);
    }

    /**
     * 循环自增一万次
     *
     * @param test
     */
    private static void forAdd(C_VolatileAndAtomic test) {
        for (int i = 0; i < 10000; i++) {
            test.num++;
        }
    }
}
