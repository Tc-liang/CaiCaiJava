package B_字节码与类加载篇.虚拟机类加载机制;

/**
 * @author Tc.l
 * @Date 2020/10/30
 * @Description: 被动引用 实例数组
 * <p>
 * 数组由jvm创建的类来管理
 */
public class TestArr {
    static {
        System.out.println("main方法所在的类初始化");
    }

    public static void main(String[] args) {
        Arr[] arrs = new Arr[1];
    }
}

class Arr {
    static {
        System.out.println("arr初始化");
    }
}
