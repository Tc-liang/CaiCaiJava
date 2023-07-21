package B_字节码与类加载篇.虚拟机字节码执行引擎;

import java.util.concurrent.TimeUnit;

/**
 * @author Tcl
 * @Date 2021/4/27
 * @Description:局部变量线程安全吗?
 */
public class LocalVariableThreadUnSafety {
    /**
     * 作用域只在方法中的局部变量线程安全
     */
    public static void localVariable1() {
        StringBuilder sb = new StringBuilder();
        sb.append("a");
        sb.append("b");
    }



    /**
     * 通过参数传入方法的局部变量可能线程不安全
     *
     * @param sb
     */
    public static void localVariable2(StringBuilder sb) {
        char[] chars = "abcdefghijkmlnopqretuvwxyz".toCharArray();
        for (int i = 0; i < chars.length; i++) {
            sb.append(chars[i]);
        }
        System.out.print(" ");
    }

    /**
     * 可变对象逃逸,线程不安全
     *
     * @return
     */
    public static StringBuilder localVariable3() {
        StringBuilder sb = new StringBuilder();
        char[] chars = "abcdefghijkmlnopqretuvwxyz".toCharArray();
        for (int i = 0; i < chars.length; i++) {
            sb.append(chars[i]);
        }
        return sb;
    }

    /**
     * 逃逸是不可变对象,线程安全
     *
     * @return
     */
    public static String localVariable4() {
        StringBuilder sb = new StringBuilder();
        sb.append("a");
        sb.append("b");
        return sb.toString();
    }


    public static void main(String[] args) throws InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                LocalVariableThreadUnSafety.localVariable2(stringBuilder);
            }).start();
        }

        TimeUnit.SECONDS.sleep(5);
        System.out.println(stringBuilder);
    }
}
