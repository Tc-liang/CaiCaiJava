package C_垃圾回收篇.GC;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/4
 * @Description:
 */
public class SystemGCTest {
    public static void main(String[] args) {
       test5();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("执行了finalize");
    }

    public static void test1() {
        byte[] bytes = new byte[5 * 1024 * 1024];
        System.gc();
    }

    public static void test2() {
        test1();
        System.gc();
    }

    public static void test3() {
        byte[] bytes = new byte[5 * 1024 * 1024];
        bytes = null;
        System.gc();
    }

    public static void test4() {
        {
            byte[] bytes = new byte[5 * 1024 * 1024];
        }
        System.gc();
    }

    public static void test5() {
        {
            byte[] bytes = new byte[5 * 1024 * 1024];
        }
        int i = 0;
        System.gc();
    }
}
