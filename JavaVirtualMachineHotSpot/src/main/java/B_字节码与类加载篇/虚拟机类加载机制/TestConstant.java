package B_字节码与类加载篇.虚拟机类加载机制;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Tc.l
 * @Date 2020/10/30
 * @Description: 被动引用调用静态常量
 */
public class TestConstant {
    static final int NUM = 555;
    static final int NUM1 = Integer.valueOf("123");
    final int NUM2 = 555;
    static final Map map = new HashMap();
    static final String s = "123";

    static {
        System.out.println("main方法所在的类初始化");
    }

    public static void main(String[] args) {
        System.out.println(TestConstant.NUM);
        ArrayList<Integer> list = new ArrayList<>();
        list.forEach(System.out::println);
    }

    public void test() {
        int i = 10;
        long l = 10L;
        double d = 20.0;
        {
            int a = 10;
            a = i + a;
        }
        long j = 10L;

    }

    public void operandStackTest() {
        int i = 10;
        byte b = 5;
        int j = i + b;
    }

    public void byteCodeIncrementTest() {
        int i1 = 10;
        i1++;

        int i2 = 10;
        ++i2;

        int i3 = 10;
        int i4 = i3++;

        int i5 = 10;
        int i6 =++i5;

        int i7=10;
        i7=i7++;

        int i8=10;
        i8=++i8;

        int i9=10;
        int i10=i9++ + ++i9;
    }
}


