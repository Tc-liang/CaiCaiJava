package I_SyncFuture;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/6/28 11:30
 * @description: DefaultFuture 无锁版
 */
public class DefaultFutureNoLock implements Future<MsgResponse> {

    public DefaultFutureNoLock(String msgId) {
        this(msgId, 200L, TimeUnit.MILLISECONDS);
    }

    public DefaultFutureNoLock(String msgId, long timeout, TimeUnit timeUnit) {
        this.msgId = msgId;
        this.timeout = timeout;
        this.timeUnit = timeUnit;

        futures.put(msgId, this);
        msgIdThreads.put(msgId, Thread.currentThread());
        startTimeMillis = System.currentTimeMillis();
    }

    /**
     * 通过MQTT发布、订阅模型通信
     * 多Java节点的情况下无法分辨消息是不是当前节点发送的
     * 因此需要唯一标识 消息ID来判断
     * 并且本地还要进行存储，因此考虑使用KV的容器进行存储
     * 由于是会被并发访问，因此使用ConcurrentHashMap
     * K存储消息ID，V存储DefaultFuture，通过消息ID可以获取DefaultFuture
     */
    private static final Map<String, DefaultFutureNoLock> futures = new ConcurrentHashMap<>();

    /**
     * 和创建default future的业务线程绑定 用于唤醒
     */
    private static final Map<String, Thread> msgIdThreads = new ConcurrentHashMap<>();

    /**
     * 消息唯一标识
     */
    private String msgId;

    /**
     * 用于判断任务是否被取消
     * true被取消  false未被取消
     */
    private boolean isCancel;

    /**
     * 启动时间用于判断是否超时
     */
    private long startTimeMillis;

    private long timeout;

    private TimeUnit timeUnit;

    /**
     * 消息通信的结果
     * 收到消息后 将消息封装成MsgResponse对象 存储到msgResponse中
     * 用于判断任务是否完成
     */
    private MsgResponse msgResponse;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        //取消任务
        futures.remove(msgId);
        msgIdThreads.remove(msgId);
        isCancel = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return isCancel;
    }

    @Override
    public boolean isDone() {
        //结果不为空 说明消息通信完成
        return Objects.nonNull(msgResponse);
    }

    @Override
    public MsgResponse get() throws InterruptedException, ExecutionException {
        return get(timeout, timeUnit);
    }

    @Override
    public MsgResponse get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
        //参数校验
        if (timeout <= 0) {
            throw new RuntimeException("超时时间有误");
        }

        //覆盖超时字段
        this.timeout = timeout;
        this.timeUnit = unit;

        //如果任务完成就返回结果 否则阻塞等待任务完成
        if (isDone()) {
            return msgResponse;
        }

        long nanos = unit.toNanos(timeout);
        LockSupport.parkNanos(this, nanos);

        //结果为空 抛出异常
        if (!isDone()){
            throw new RuntimeException("超时");
        }


        return msgResponse;
    }


    /**
     * 接收消息
     *
     * @param msg
     * @return
     */
    public static boolean received(MsgResponse msg) {
        //接收时删除
        DefaultFutureNoLock future = futures.remove(msg.getMsgId());
        if (Objects.isNull(future)) {
            return false;
        }

        //唤醒
        future.doReceived(msg);
        return true;
    }


    private void doReceived(MsgResponse msg) {
        msgResponse = msg;
        LockSupport.unpark(msgIdThreads.get(msgId));
    }


    /**
     * Job 补偿 清理超时任务
     */
    public void startJob() {
        Thread timeoutJobThread = new Thread(() -> {
            while (true) {
                try {
                    for (DefaultFutureNoLock future : futures.values()) {
                        if (future == null || future.isDone()) {
                            continue;
                        }

                        //如果超时就去清理 以防内存泄漏
                        if (System.currentTimeMillis() - future.startTimeMillis > future.timeUnit.toMillis(timeout)) {
                            MsgResponse msgResponse = new MsgResponse();
                            msgResponse.setMsgId(future.msgId);
                            msgResponse.setMsgBodyJson("timeout");
                            DefaultFutureNoLock.received(msgResponse);
                        }
                    }
                    Thread.sleep(10000);
                } catch (Throwable e) {
                    //日志
                }
            }
        }, "超时任务线程");

        timeoutJobThread.setDaemon(true);
        timeoutJobThread.start();
    }

}

