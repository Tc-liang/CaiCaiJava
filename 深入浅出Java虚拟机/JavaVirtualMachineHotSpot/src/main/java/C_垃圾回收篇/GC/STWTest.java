package C_垃圾回收篇.GC;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/4
 * @Description:测试STW
 */
public class STWTest {
    public static void main(String[] args) {
        new Thread(() -> {
            long start = System.currentTimeMillis();
            while (true) {
                long millis = System.currentTimeMillis() - start;
                System.out.println(millis);
                 try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "计时线程").start();

//        System.out.println(Runtime.getRuntime().availableProcessors());

        new Thread(()->{
            ArrayList<Object> list = new ArrayList<>();
            while (true){
                for (int i = 0; i < 2000; i++) {
                    byte[] bytes = new byte[1024 * 1024];
                    list.add(bytes);
                }

                if (list.size() > 5000){
                    list.clear();
                    System.gc();
                }
            }
        },"故意制造垃圾回收发生STW的线程").start();
    }
}
