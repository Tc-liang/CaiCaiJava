package C_垃圾回收篇.垃圾收集器与内存分配策略;

/**
 * @author Tc.l
 * @Date 2020/11/21
 * @Description:
 */
public class MinorGCTest {
    private static final int _1MB = 1024 * 1024;

    /**
     * 对象优先在Eden分配
     * -Xmx20m -Xms20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:+UseSerialGC
     */
    public static void test1() {
        byte[] byte1, byte2, byte3, byte4;
        byte1 = new byte[2 * _1MB];
        byte2 = new byte[2 * _1MB];
        byte3 = new byte[2 * _1MB];
        byte4 = new byte[4 * _1MB];
    }

    /**
     * 大对象直接加入老年代
     * -Xmx20m -Xms20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:+UseSerialGC
     * -XX:PretenureSizeThreshold=3145728
     * (3145728=3MB)大于该大小的对象直接分配在老年代 ,该参数只对Serial/ParNew收集器管用
     * Server模式下默认的是Parallel Scavenge + Parallel Old
     */
    public static void test2() {
        byte[] bytes = new byte[4 * _1MB];

    }


    /**
     * 长期存活对象进入老年代
     * -Xmx20m -Xms20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:+UseSerialGC
     * -XX:MaxTenuringThreshold=1
     * 年龄为1就放入老年代
     */
    public static void test3() {
        byte[] bytes1, bytes2, bytes3;
        bytes1 = new byte[_1MB / 4];
        bytes2 = new byte[4 * _1MB];
        bytes3 = new byte[4 * _1MB];
        bytes3 = null;
        bytes3 = new byte[4 * _1MB];
    }

    /**
     * -Xmx20m
     * -Xms20m
     * -Xmn10m
     * -XX:+PrintGCDetails
     * -XX:SurvivorRatio=8
     * -XX:+UseSerialGC
     * -XX:MaxTenuringThreshold=15
     */
    public static void test4() {
        byte[] bytes1, bytes2, bytes3, bytes4;
        bytes1 = new byte[_1MB / 4];
        //注释掉后 老年代从51降低为48 (bytes1+bytes2 有 Survive区一半大小所以它们会被放到老年代)
//        bytes2 = new byte[_1MB / 4];
        bytes3 = new byte[_1MB * 4];
        bytes4 = new byte[_1MB * 4];
        bytes4 = null;
        bytes4 = new byte[_1MB * 4];
    }

    public static void test5(){

    }

    public static void main(String[] args) {
        test1();
    }
}
