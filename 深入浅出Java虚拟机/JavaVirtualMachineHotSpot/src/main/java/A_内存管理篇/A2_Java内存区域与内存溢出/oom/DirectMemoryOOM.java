package A_内存管理篇.A2_Java内存区域与内存溢出.oom;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author Tc.l
 * @Date 2020/10/27
 * @Description: 测试直接内存OOM
 * -XX:MaxDirectMemorySize=10m
 * -Xmx20m
 */
public class DirectMemoryOOM {
    static final int _1MB = 1024 * 1024;

    public static void main(String[] args) throws IllegalAccessException {
        Field declaredField = Unsafe.class.getDeclaredFields()[0];
        declaredField.setAccessible(true);
        Unsafe unsafe = (Unsafe) declaredField.get(null);
        while (true) {
            unsafe.allocateMemory(_1MB);
        }
    }
}
