package A_volatile;

/**
 * @Author: Caicai
 * @Date: 2023-08-23 22:44
 * @Description:volatile与伪共享问题
 */
public class D_VolatileAndFalseSharding {
    @sun.misc.Contended
    private volatile int i1 = 0;
    @sun.misc.Contended
    private volatile int i2 = 0;
    
    public static void main(String[] args) throws InterruptedException {
        D_VolatileAndFalseSharding test = new D_VolatileAndFalseSharding();
        int count = 1_000_000_000;
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                test.i1++;
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                test.i2++;
            }
        });

        long start = System.currentTimeMillis();

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        //31910 i1:1000000000 i2:1000000000

        //使用@sun.misc.Contended解决伪共享问题  需要携带JVM参数 -XX:-RestrictContended
        //5961 i1:1000000000 i2:1000000000
        System.out.println((System.currentTimeMillis() - start) + " i1:"+ test.i1 + " i2:"+ test.i2);
    }

}