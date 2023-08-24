package D_性能调优篇.JVM参数选项;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tcl
 * @Date 2021/5/22
 * @Description:
 * -Xmx60m -Xms60m
 * -XX:+HeapDumpOnOutOfMemoryError
 * -XX:+HeapDumpBeforeFullGC
 * -XX:HeapDumpPath
 */
public class OOMOption {
    static List<byte[]> list = new ArrayList<>();

    static final int _1MB = 1024 * 1024;

    public static void main(String[] args) {
        for (int i = 0; i < 500; i++) {
            list.add(new byte[_1MB]);
        }
    }
}
