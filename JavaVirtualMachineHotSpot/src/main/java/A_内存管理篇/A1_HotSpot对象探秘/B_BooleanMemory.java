package A_内存管理篇.A1_HotSpot对象探秘;

import org.openjdk.jol.info.ClassLayout;

/**
 * @author Tcl
 * @Date 2021/5/13
 * @Description: 测试对象内存 boolean 与 内部类
 * boolean 虽然只需要一位判断真假，但JVM规范未规定要用多大空间，hotspot 64位使用1字节存储值，然后还有补3字节对齐
 * 引用使用4字节记录其地址
 * 内部类实例数据中还会记录this
 */
public class B_BooleanMemory {
    //1 B
    private boolean b ;

    //4 B
    private Boolean b2 ;

    //4 B
    private Object o ;

    private boolean[] arr = new boolean[5];

    class  InnerClass{
       private int i;
    }
    public static void main(String[] args) {
        //内存 12（对象头）+1（boolean）+3(对齐)+4（Boolean）+4(Object)+4(数组)+4对其填充 = 32
        B_BooleanMemory testBoolean = new B_BooleanMemory();
        System.out.println(ClassLayout.parseInstance(testBoolean).toPrintable());

        //内部类 内存 12+4字段+4 this +4对齐
        InnerClass innerClass = testBoolean.new InnerClass();
        System.out.println(ClassLayout.parseInstance(innerClass).toPrintable());
    }
}

