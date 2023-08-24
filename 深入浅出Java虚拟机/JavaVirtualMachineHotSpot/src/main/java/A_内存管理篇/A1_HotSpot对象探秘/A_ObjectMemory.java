package A_内存管理篇.A1_HotSpot对象探秘;

import org.openjdk.jol.info.ClassLayout;


/**
 * @author Tc.l
 * @Date 2020/11/24
 * @Description:
 * 探秘对象占用内存
 * hotspot  64位
 * 对象分为：对象头(mark word 8Byte + class word 类型指针8Byte压缩后4Byte + 是否数组，是则4Byte记录长度)、实例数据、对其填充（数据补充8Byte的倍数）
 * 注意：对象中占用的内存存储的是数据，不存储字段类型的信息（类信息存储在方法区klass）
 */
public class A_ObjectMemory extends Super{
    private int i = 5;
    public static void main(String[] args) {
        //Object 内存 12+0+4 = 16
        System.out.println(ClassLayout.parseInstance(new Object()).toPrintable());

        //数组 内存 16+4*5+4 = 40
        System.out.println(ClassLayout.parseInstance(new int[5]).toPrintable());

        //父子类，子类存储父类字段值  12+8（4+4）+4 = 24
        System.out.println(ClassLayout.parseInstance(new A_ObjectMemory()).toPrintable());
    }
}

class Super{
    private int m = 10;
}
