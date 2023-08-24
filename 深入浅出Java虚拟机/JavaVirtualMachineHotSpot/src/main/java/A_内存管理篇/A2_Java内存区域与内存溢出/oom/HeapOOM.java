package A_内存管理篇.A2_Java内存区域与内存溢出.oom;


import java.util.ArrayList;

/**
 * @author Tc.l
 * @Date 2020/10/27
 * @Description: 测试堆内存溢出
 * -Xms20m 初始化堆内存
 * -Xmx20m 最大堆内存
 * -XX:+HeapDumpOnOutOfMemoryError DumpOOM的内存快照
 *
 * jvisualvm 或者 jstat -gcutil查看内存情况
 *
 */
public class HeapOOM {

    private byte[] bytes = new byte[1024];

    public static void main(String[] args) {
        ArrayList<HeapOOM> list = new ArrayList();
        while (true){
            list.add(new HeapOOM());
        }
    }
}
