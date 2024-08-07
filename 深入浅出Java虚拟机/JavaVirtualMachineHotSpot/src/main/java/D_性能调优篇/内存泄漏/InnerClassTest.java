package D_性能调优篇.内存泄漏;

import org.openjdk.jol.info.ClassLayout;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/20
 * @Description:
 */
public class InnerClassTest {

    class InnerClass {

    }

    public InnerClass getInnerInstance() {
        return this.new InnerClass();
    }

    public static void main(String[] args) {
        InnerClass innerInstance = null;

        {
            InnerClassTest innerClassTest = new InnerClassTest();
            innerInstance = innerClassTest.getInnerInstance();
            System.out.println("===================外部实例对象内存布局==========================");
            System.out.println(ClassLayout.parseInstance(innerClassTest).toPrintable());

            System.out.println("===================内部实例对象内存布局===========================");
            System.out.println(ClassLayout.parseInstance(innerInstance).toPrintable());
        }

        //省略很多代码.....
    }
}
