package B_字节码与类加载篇.虚拟机类加载机制;


/**
 * @author Tc.l
 * @Date 2020/10/30
 * @Description: 被动引用 子类调用父类静态变量
 */
public class TestMain {
    static {
        System.out.println("main方法所在的类初始化");
    }

    public static void main(String[] args) {
//        System.out.println(Sup.i);
        try {
            //隐式类加载只会等到第一次使用时才初始化
            ClassLoader.getSystemClassLoader().loadClass("B_字节码与类加载篇.虚拟机类加载机制.Sub");

            Sub sub = new Sub();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

class Sub extends Sup {
    static {
        int i = 1;

        System.out.println("子类初始化");
    }
}

class Sup {
    static {
        System.out.println("父类初始化");
    }

    static int i = 100;
}


