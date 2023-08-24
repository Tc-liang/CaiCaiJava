package C_垃圾回收篇.引用;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Tc.l
 * @Date 2020/11/19
 * @Description: 虚引用测试
 */
public class PhantomReferenceTest {
    private static PhantomReferenceTest reference;
    private static ReferenceQueue queue;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("调用finalize方法");
        reference = this;
    }

    public static void main(String[] args) {
        reference = new PhantomReferenceTest();
        queue = new ReferenceQueue<>();
        PhantomReference<PhantomReferenceTest> phantomReference = new PhantomReference<>(reference, queue);

        Thread thread = new Thread(() -> {
            PhantomReference<PhantomReferenceTest> r = null;
            while (true) {
                if (queue != null) {
                    r = (PhantomReference<PhantomReferenceTest>) queue.poll();
                    //说明被回收了,得到通知
                    if (r != null) {
                        System.out.println(r);
                        System.out.println("实例被回收");
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        //null
        System.out.println(phantomReference.get());

        try {
            System.out.println("第一次gc 对象可以自救");
            reference = null;
            System.gc();
            TimeUnit.SECONDS.sleep(1);
            if (reference == null) {
                System.out.println("object is dead");
            } else {
                System.out.println("object is alive");
            }
            reference = null;
            System.out.println("第二次gc 对象无法自救");
            System.gc();
            TimeUnit.SECONDS.sleep(1);
            if (reference == null) {
                System.out.println("object is dead");
            } else {
                System.out.println("object is alive");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
