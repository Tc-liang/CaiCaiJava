package D_ThreadPool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyThreadPoolFactory implements ThreadFactory {

    private AtomicInteger threadNumber = new AtomicInteger(1);

    private ThreadGroup group;

    private String namePrefix = "";

    public MyThreadPoolFactory(String group) {
        this.group = new ThreadGroup(group);
        namePrefix = group + "-thread-pool-";
    }


    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0);
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println(t.getName() + ":" + e);
            }
        });

        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        return t;
    }

}