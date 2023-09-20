package A_Volatile;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Caicai
 * @Date: 2023-08-22 23:14
 * @Description:volatile的可见性
 */
public class B_VolatileAndVisibility {

    static int nonVolatileNumber = 0;

    static volatile int volatileNumber;

    public static void main(String[] args) throws InterruptedException {
        testNonVolatile();
//        testVolatile();
    }

    /**
     * 测试不使用volatile
     * 主线程修改变量值后,其他线程不能感知变量改变 一直循环
     *
     * @throws InterruptedException
     */
    public static void testNonVolatile() throws InterruptedException {
        new Thread(() -> {
            while (nonVolatileNumber == 0) {

            }
        }).start();

        TimeUnit.SECONDS.sleep(1);
        nonVolatileNumber = 100;
    }

    /**
     * 测试使用volatile
     * 主线程修改变量值后,其他线程感知变量改变 退出循环
     * 对改变的内存可见
     *
     * @throws InterruptedException
     */
    public static void testVolatile() throws InterruptedException {
        new Thread(() -> {
            while (volatileNumber == 0) {

            }
        }).start();

        TimeUnit.SECONDS.sleep(1);
        volatileNumber = 100;
    }
}
