package D_性能调优篇.JVM参数选项;

import java.util.concurrent.TimeUnit;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/22
 * @Description:
 */
public class Test {
    public static void main(String[] args) {
        while (true){
            try {
                TimeUnit.SECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
