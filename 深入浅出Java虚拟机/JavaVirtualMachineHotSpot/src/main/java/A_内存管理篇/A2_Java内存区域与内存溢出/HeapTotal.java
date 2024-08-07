package A_内存管理篇.A2_Java内存区域与内存溢出;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/4/27
 * @Description:测试堆内存容量
 * -Xms1024m  修改初始化内存
 * -Xmx1024m  修改使用最大内存
 * -XX:+PrintGCDetails  打印垃圾回收信息
 * -Xms1024m -Xmx1024m -XX:+PrintGCDetails
 */
public class HeapTotal {
    public static void main(String[] args) {
        //JVM试图使用最大内存
        long maxMemory = Runtime.getRuntime().maxMemory();
        //JVM初始化总内存
        long totalMemory = Runtime.getRuntime().totalMemory();
        System.out.println("JVM试图使用最大内存-->"+maxMemory+"B 或"+(maxMemory/1024/1024)+"MB");
        System.out.println("JVM初始化总内存-->"+totalMemory+"B 或"+(totalMemory/1024/1024)+"MB");
    }
}
