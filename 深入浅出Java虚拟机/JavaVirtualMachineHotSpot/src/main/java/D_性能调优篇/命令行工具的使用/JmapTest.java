package D_性能调优篇.命令行工具的使用;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author Tcl
 * @Date 2021/5/18
 * @Description:
 * -Xms60m -Xmx60m -XX:SurvivorRatio=8 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=d:\3.hprof
 */
public class JmapTest {
    public static void main(String[] args) {
        ArrayList<byte[]> list = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            //100kb
            byte[] bytes = new byte[1024 * 100];
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            list.add(bytes);
        }
    }
}
