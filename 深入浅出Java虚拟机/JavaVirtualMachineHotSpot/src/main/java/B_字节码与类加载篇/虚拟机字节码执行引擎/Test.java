package B_字节码与类加载篇.虚拟机字节码执行引擎;

/**
 * @author Tc.l
 * @Date 2020/11/10
 * @Description:
 */
public class Test {
    public void add(int a){
        a=a+2;
    }
    public static void main(String[] args) {
        new Test().add(10);
    }
}
