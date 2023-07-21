package A_内存管理篇.A2_Java内存区域与内存溢出.字符串与字符串常量池;


/**
 * @author Tcl
 * @Date 2021/5/1
 * @Description:
 */
public class StringTest {
    public static void main(String[] args) {
        test2();
    }

    private static void test1() {
        //s1是常量拼接,指向字符串常量池中的字面量"abc"对象
        String s1 = "a" + "b" + "c";
        String s2 = "a";

        //s3拼接中不是常量拼接,会采用StringBuilder拼接,指向堆中"abc"的String引用
        String s3 = s2 + "bc";
        final String s4 = "a";

        //s5拼接了final修饰的s4,s4指向引用不能改变,所以当常量,最后s5指向字符串常量池中的字面量"abc"对象
        String s5 = s4 + "bc";

        //s1指向字符串常量池的字面量对象"abc" s3指向堆中"abc"的String引用 所以引用地址不相等
        System.out.println(s1 == s3);//false

        //s1,s5都指向字符串常量池中的字面量"abc"对象,字符串常量池中的字面量对象采用享元模式的思想,都是唯一的 所以引用地址相等
        System.out.println(s1 == s5);//true
    }


    public static void test2() {
        //字符串常量池中有a字面量对象的情况
        String s0 = new String("a");
        String s1 = s0.intern();
        //s0所引用的字符串在堆中，而s1所用的字符串在字符串常量池中
        System.out.println(s0 == s1);//false

        //字符串常量池中没有aa字面量对象的情况
        String s2 = new String("a") + new String("a");
        //字符串常量池中没有(equals为false),所以在字符串常量池中创建的字面量aa对象会利用堆中s2的引用(字符串常量池字面量aa对象实际指向与s2相同)
        String s4 = s2.intern();
        System.out.println(s2 == s4);//true
        System.out.println(s2 == "aa");//true
    }

    public static void test3() {
        String abc = new String("ab");
    }

    public static void test4() {
        String abc = new String("a") + new String("b");
    }
}
