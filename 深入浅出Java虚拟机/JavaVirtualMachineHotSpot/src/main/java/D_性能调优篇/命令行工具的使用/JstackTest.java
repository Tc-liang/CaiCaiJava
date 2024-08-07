package D_性能调优篇.命令行工具的使用;

import java.util.concurrent.TimeUnit;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/18
 * @Description:
 */
public class JstackTest {

    public static void main(String[] args) {
        JstackTest j1 = new JstackTest();
        JstackTest j2 = new JstackTest();

        new Thread(()->{
            synchronized (j1){
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (j2){

                }
            }
        },"JstackTest线程A").start();


        new Thread(()->{
            synchronized (j2){
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (j1){

                }
            }
        },"JstackTest线程B").start();
    }
}
