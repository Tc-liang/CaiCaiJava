package B_字节码与类加载篇.虚拟机字节码执行引擎;

/**
 * @author Tcl
 * @Date 2021/4/26
 * @Description:
 */
public class Father {
    public static void staticMethod(){
        System.out.println("father static method");
    }

    public final void finalMethod(){
        System.out.println("father final method");
    }

    public Father() {
        System.out.println("father init method");
    }

    public void overrideMethod(){
        System.out.println("father override method");
    }
}
