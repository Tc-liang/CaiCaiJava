package G_ThreadLocal;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: Caicai
 * @Date: 2023-09-21 21:04
 * @Description:
 */
public class ThreadLocalTest {

    @Test
    public void testThreadLocal(){
        ThreadLocal<String> tl = new ThreadLocal<>();

        new Thread(()->{
            test(tl,"123321");
        }).start();

        test(tl,"123");
    }

    @Test
    public void testInheritableThreadLocal(){
        InheritableThreadLocal<String> itl = new InheritableThreadLocal<>();

        itl.set("main");

        new Thread(()->{
            //main
            System.out.println(itl.get());
        }).start();
    }


    @Test
    public void testAtomic(){
        AtomicInteger atomicInteger = new AtomicInteger();
        ThreadLocal<AtomicInteger> itl = new ThreadLocal<>();

        itl.set(atomicInteger);
        AtomicInteger atomicInteger1 = itl.get();
        System.out.println(atomicInteger1.getAndIncrement());

        new Thread(()->{
            itl.set(atomicInteger);
            AtomicInteger atomicInteger2 = itl.get();
            System.out.println(atomicInteger2.getAndIncrement());
        }).start();
    }

    private void test(ThreadLocal<String> tl,String value) {
        tl.set(value);
        System.out.println(Thread.currentThread().getName()+" "+ tl.get());
        tl.remove();
    }
}
