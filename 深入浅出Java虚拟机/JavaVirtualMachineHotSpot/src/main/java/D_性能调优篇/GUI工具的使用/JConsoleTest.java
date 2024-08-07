package D_性能调优篇.GUI工具的使用;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/18
 * @Description:
 * -Xms1g -Xmx1g -XX:SurvivorRatio=8
 * -Dcom.sun.management.jmxremote 
 * -Dcom.sun.management.jmxremote.port=8011
 * -Dcom.sun.management.jmxremote.ssl=false
 * -Dcom.sun.management.jmxremote.authenticate=false
 */
public class JConsoleTest {
    byte[] bytes = new byte[new Random().nextInt(1) * 1024 * 100];
    public static void main(String[] args) {
        ArrayList<byte[]> list = new ArrayList<>(1000);
        while (true){
            JConsoleTest test = new JConsoleTest();
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            list.add(test.bytes);
        }
    }
}
