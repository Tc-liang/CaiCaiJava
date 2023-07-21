package A_内存管理篇.A2_Java内存区域与内存溢出.逃逸分析;

import java.util.concurrent.TimeUnit;

/**
 * @author Tcl
 * @Date 2021/4/28
 * @Description:
 * 逃逸分析带来的优化--测试栈上分配内存
 * 5ms
 * -XX:-DoEscapeAnalysis 关闭
 * -XX:+DoEscapeAnalysis 开启
 */
public class StackMemory {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        for (int i = 0; i < 999999999; i++) {
            memory();
        }

        System.out.println("花费时间:"+(System.currentTimeMillis()-start)+"ms");
    }

    private static void memory(){
        for (int i = 0; i < 1000000000; i++) {
            StackMemory stackMemory = new StackMemory();
        }
    }
}
