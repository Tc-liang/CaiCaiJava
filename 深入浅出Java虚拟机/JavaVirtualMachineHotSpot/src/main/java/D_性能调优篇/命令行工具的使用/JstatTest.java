package D_性能调优篇.命令行工具的使用;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/17
 * @Description:
 * -Xms60m -Xmx60m -XX:SurvivorRatio=8
 */
public class JstatTest {
    public static void main(String[] args) {
        ArrayList<byte[]> list = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            //100kb
            byte[] bytes = new byte[1024 * 100];
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            list.add(bytes);
        }
    }
}
