package _9通用编程;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/2 11:50
 * @description:
 */
public class I接口优于反射机制 {
    public static void main(String[] args) throws Exception {
        Class<? extends Set<String>> cl = (Class<? extends Set<String>>) Class.forName("java.util.HashSet");
//        Class<? extends Set<String>> cl = (Class<? extends Set<String>>) Class.forName("java.util.TreeSet");
        Constructor<? extends Set<String>> cons = cl.getDeclaredConstructor();
        Set<String> s = cons.newInstance();
        s.addAll(Arrays.asList("a", "aa", "bbb", "qqq", "asd"));
        //使用TreeSet:[a, aa, asd, bbb, qqq]
        //使用HashSet:[aa, qqq, a, bbb, asd]
        System.out.println(s);
    }
}
