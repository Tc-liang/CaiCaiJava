package B_字节码与类加载篇.前端编译与优化;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Tcl
 * @Date 2021/5/24
 * @Description:
 */
public class Test1 {


    private final int i = 1;
    public int test2(){
        if (i==1){
            return 1;
        }else {
            return 0;
        }
    }

    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        list.stream().map((l) -> {
            if (l.equals(2)) {
                l += 1;
            }
            return l;
        });



//        Object[] o = new String[10];
//        o[0] = 1;

//        ArrayList arr = new ArrayList();
//        arr.add(1);
//        arr.add("123");
    }


    public void test0(){
        int i = 100;
        char c1 = 'a';
        int i2 = 1 + 2;//编译成 int i2 = 3 常量折叠优化
//        char c2 = i + c1; //编译错误 标注检查 检查语法静态信息

        System.out.println(i);
        System.out.println(c1);
        System.out.println(i2);
    }


    @org.junit.Test
    public void test1() {
        Integer a = 1;
        Integer b = 2;
        Integer c = 3;
        Integer d = 3;
        Integer e = 321;
        Integer f = 321;
        Long g = 3L;

        System.out.println(c == d);
        System.out.println(e == f);
        System.out.println(c == (a + b));
        System.out.println(c.equals(a + b));
        System.out.println(g == (b + a));
        System.out.println(g.equals(a + b));
    }

    @Test
    public void testLong(){
        new ArrayList<Integer>(-10);
    }


}
