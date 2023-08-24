package A_volatile;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @Author: Caicai
 * @Date: 2023-08-22 23:39
 * @Description: volatile与指令重排序
 * <p>
 * 使用unsafe的内存屏障后不再发生
 */
public class A_VolatileAndReorder {
    static int a, b, x, y;

    public static void main(String[] args) throws Exception {
        long count = 0;
        Unsafe unsafe = getUnsafe();
        while (true) {
            count++;
            a = 0;
            b = 0;
            x = 0;
            y = 0;
            Thread thread1 = new Thread(() -> {
                a = 1;
//                unsafe.fullFence();
                x = b;
            });
            Thread thread2 = new Thread(() -> {
                b = 1;
//                unsafe.fullFence();
                y = a;
            });
            thread1.start();
            thread2.start();

            try {
                thread1.join();
                thread2.join();
            } catch (Exception e) {
            }

            if (x == 0 && y == 0) {
                break;
            }
        }
        //count=118960,x=0,y=0
        System.out.println("count=" + count + ",x=" + x + ",y=" + y);
    }

    /**
     * 反射获取Unsafe类
     *
     * @return
     * @throws Exception
     */
    private static Unsafe getUnsafe() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (Unsafe) theUnsafe.get(null);
    }
}
