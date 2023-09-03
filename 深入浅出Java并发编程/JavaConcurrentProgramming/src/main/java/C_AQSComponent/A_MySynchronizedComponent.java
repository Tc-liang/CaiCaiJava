package C_AQSComponent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author: Caicai
 * @Date: 2023-09-02 18:25
 * @Description: 自定义同步组件--可重入锁
 */
public class A_MySynchronizedComponent implements Lock {

    public A_MySynchronizedComponent() {
        sync = new Sync();
    }

    private Sync sync;


    /**
     * 初始化同步状态为0
     * 获取同步状态时,CAS将同步状态从0改为1算成功或者可重入累加次数
     * 释放同步状态时,只有1->0才是真正释放 其他可重入情况就自减
     */
    static class Sync extends AbstractQueuedSynchronizer {
        /**
         * 判断当前线程是否持有同步状态
         *
         * @return
         */
        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        /**
         * 尝试获取同步状态
         *
         * @param arg 获取同步状态的数量
         * @return
         */
        @Override
        protected boolean tryAcquire(int arg) {
            //1.获取同步状态
            int state = getState();
            //2.如果有同步状态则CAS替换 0->1
            if (state == 0) {
                if (compareAndSetState(state, 1)) {
                    //替换成功 说明获取到同步状态 设置当前获取同步状态线程
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
            } else if (getExclusiveOwnerThread() == Thread.currentThread()) {
                //3.没有同步状态  查看获取同步资源的线程是否为当前线程  可重入  累加重入次数
                setState(state + arg);
                return true;
            }

            //其他情况就是没获取到同步状态
            return false;
        }

        /**
         * 尝试释放同步状态
         *
         * @param arg 释放同步状态的数量
         * @return
         */
        @Override
        protected boolean tryRelease(int arg) {
            //目标状态
            int targetState = getState() - arg;

            //真正释放锁
            if (targetState == 0) {
                setExclusiveOwnerThread(null);
                setState(targetState);
                return true;
            }

            //其他情况 扣减状态
            setState(targetState);
            return false;
        }
    }


    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.new ConditionObject();
    }

}
