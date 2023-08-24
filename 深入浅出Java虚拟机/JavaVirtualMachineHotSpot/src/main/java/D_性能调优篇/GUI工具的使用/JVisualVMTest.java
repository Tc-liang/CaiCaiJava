package D_性能调优篇.GUI工具的使用;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author Tcl
 * @Date 2021/5/19
 * @Description:
 * -Xmx600m -Xms600m -XX:SurvivorRatio=9
 */
public class JVisualVMTest {
    public static void main(String[] args) {
        ArrayList<byte[]> list = new ArrayList<>(1000);
        while (true){
            try {
                byte[] b1 = new byte[1024];
                list.add(b1);
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
