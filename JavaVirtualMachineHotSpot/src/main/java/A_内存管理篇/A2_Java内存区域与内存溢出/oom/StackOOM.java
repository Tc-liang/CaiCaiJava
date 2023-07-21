package A_内存管理篇.A2_Java内存区域与内存溢出.oom;

import java.util.concurrent.TimeUnit;

/**
 * @author Tc.l
 * @Date 2020/10/27
 * @Description: 测试栈内存溢出OOM
 * -Xss20m
 */
public class StackOOM {
    public void testStackOOM(){
        while (true){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            TimeUnit.SECONDS.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.start();
        }
    }

    public static void main(String[] args) {
        StackOOM stackOOM = new StackOOM();
        stackOOM.testStackOOM();
    }
}
