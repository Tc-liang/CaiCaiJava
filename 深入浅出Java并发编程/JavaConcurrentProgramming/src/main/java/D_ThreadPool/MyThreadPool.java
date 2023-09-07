package D_ThreadPool;

import java.util.Objects;
import java.util.concurrent.*;

public class MyThreadPool extends ThreadPoolExecutor {

    public MyThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public MyThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        //Throwable为空 可能是submit提交 如果runnable为future 则捕获get
        if (Objects.isNull(t) && r instanceof Future<?>) {
            try {
                Object res = ((Future<?>) r).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                t = e;
            }
        }

        if (Objects.nonNull(t)) {
            System.out.println(Thread.currentThread().getName() + ": " + t.toString());
        }
    }

}