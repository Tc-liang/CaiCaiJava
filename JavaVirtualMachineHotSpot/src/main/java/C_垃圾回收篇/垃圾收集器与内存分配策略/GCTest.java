package C_垃圾回收篇.垃圾收集器与内存分配策略;

import java.util.concurrent.TimeUnit;

/**
 * @author Tcl
 * @Date 2021/5/5
 * @Description:查看垃圾收集器
 * -XX:+PrintCommandLineFlags 启动打印JVM参数
 * -XX:+UseSerialGC  新生代使用Serial GC 老年代使用 Serial Old GC
 * -XX:+UseParNewGC -XX:ParallelGCThreads=8
 * -XX:+UseConcMarkSweepGC
 */
public class GCTest {

    public static void main(String[] args) {
        while (true){
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
