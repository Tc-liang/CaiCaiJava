package B_字节码与类加载篇.虚拟机类加载机制;


/**
 * @author Tc.l
 * @Date 2020/10/30
 * @Description: 多线程保证<clinit>类构造器只执行一次
 */
public class TestJVM {
    static class A {
        static {
            if (true) {
                System.out.println(Thread.currentThread().getName() + "<clinit> init");
                while (true) {

                }
            }
        }
    }

    public static void main(String[] args) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + "start");
                A a = new A();
                System.out.println(Thread.currentThread().getName() + "end");
            }
        };
        new Thread(runnable, "1号线程").start();
        new Thread(runnable, "2号线程").start();
    }

}


