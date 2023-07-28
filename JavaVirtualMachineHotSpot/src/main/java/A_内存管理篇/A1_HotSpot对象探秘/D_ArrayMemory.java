package A_内存管理篇.A1_HotSpot对象探秘;

import org.openjdk.jol.info.ClassLayout;

/**
 * @Author: Caicai
 * @Date: 2023-07-28 22:04
 * @Description: 数组对象空间
 * <p>
 * 数组对象的对象头中会多4字节记录数组长度
 * 一维数组申请的地址在空间上是连续的
 * 二维数组可以当作一个对象中包含多个对象（一维数组），其地址不一定是连续的，只是会记录一维数组对象的地址
 */
public class D_ArrayMemory {
    public static void main(String[] args) {
        //一维数组
        int[] arr1 = new int[]{7,8,9};
        System.out.println(ClassLayout.parseInstance(arr1).toPrintable());

        //二维数组
        int[][] arr2 = {{1, 2, 3}, {4, 5, 6}};
        System.out.println(ClassLayout.parseInstance(arr2).toPrintable());
    }
}
