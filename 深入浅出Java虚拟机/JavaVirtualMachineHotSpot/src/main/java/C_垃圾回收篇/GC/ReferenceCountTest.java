package C_垃圾回收篇.GC;

/**
 * @author Tcl
 * @Date 2021/5/1
 * @Description:测试Java不采用引用计数算法 -XX:+PrintGCDetails
 */
public class ReferenceCountTest {
    //占用内存
    private static final byte[] MEMORY = new byte[1024 * 1024 * 2];

    private ReferenceCountTest reference;

    public static void main(String[] args) {
        ReferenceCountTest a = new ReferenceCountTest();
        ReferenceCountTest b = new ReferenceCountTest();
        //循环引用
        a.reference = b;
        b.reference = a;

        a = null;
        b = null;
//        System.gc();
    }
}
