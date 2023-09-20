package A_Thread;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * @author Tc.l
 * @Date 2020/12/1
 * @Description:
 * 启动Java程序会有多少个线程
 */
public class JavaProgramThread {
    public static void main(String[] args) throws InterruptedException {
        //获取Java线程管理Bean
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        //不获取 monitors 和 synchronized 信息
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);
        for (ThreadInfo threadInfo : threadInfos) {
            System.out.println("["+threadInfo.getThreadId()+"]"+threadInfo.getThreadName());
        }
        Thread thread = Thread.currentThread();
        thread.setPriority(9);

        while (true){
            Thread.sleep(1000000000);
        }
    }
}
