package B_字节码与类加载篇.虚拟机类加载机制;

import java.util.Arrays;
import java.util.List;

/**
 * @author Tcl
 * @Date 2021/5/6
 * @Description:
 */
public class TestApplication {
    private static ThreadLocal local = ThreadLocal.withInitial(() -> {
        return 8;
    });

    public static void main(String[] args) {
//        System.out.println(local.get());

        String property = System.getProperty("java.class.path");
        List<String> list = Arrays.asList(property.split(";"));
        list.forEach((t) -> {
            System.out.println("应用类加载器" + t);
        });

    }
}
