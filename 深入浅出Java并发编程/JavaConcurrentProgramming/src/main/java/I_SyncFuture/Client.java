package I_SyncFuture;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/6/28 8:36
 * @description:
 */
public class Client {
    /**
     * 模拟MQTT通信时 A端 接收消息的 Topic
     */
    private static final LinkedBlockingQueue<String> mqttTopicA = new LinkedBlockingQueue<>();

    /**
     * 模拟MQTT通信时 B端 接收消息的 Topic
     */
    private static final LinkedBlockingQueue<String> mqttTopicB = new LinkedBlockingQueue<>();


    public static void main(String[] args) {
//        defaultFutureTest();
        defaultFutureNoLockTest();
    }

    private static void defaultFutureNoLockTest() {
        //1.开启消费线程 模拟B端消费消息
        Thread bConsumerThread = new Thread(() -> {
            while (true) {

                //获取消息
                String msgId = null;
                try {
                    msgId = mqttTopicB.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //消费..
                System.out.println("2." + Thread.currentThread().getName() + "消费消息：" + msgId);


                //模拟网络拥塞
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }

                //消费完响应
                mqttTopicA.offer(msgId);

            }
        }, "B端消费线程");
        bConsumerThread.setDaemon(true);
        bConsumerThread.start();

        //2.开启接收线程 模拟A端接收消息
        Thread aReceivedThread = new Thread(() -> {
            while (true) {

                //获取消息
                String msgId = null;
                try {
                    msgId = mqttTopicA.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //唤醒业务处理线程
                MsgResponse msgResponse = new MsgResponse();
                msgResponse.setMsgId(msgId);
                //实际上消息通信会传输消息内容的，这里图方便 不想解析消息 就用msgID当作消息内容传输
                msgResponse.setMsgBodyJson("JSON消息内容");
                if (DefaultFutureNoLock.received(msgResponse)) {
                    System.out.println("3." + Thread.currentThread().getName() + "唤醒消息：" + msgId);
                }
            }
        }, "A端接收消息线程");
        aReceivedThread.setDaemon(true);
        aReceivedThread.start();


        //3.业务处理线程发送消息
        String msgId = UUID.randomUUID().toString();
        System.out.println("1.A端业务线程开始发送消息");
        MsgResponse msgResponse = sendMsgNoLock(msgId);
        System.out.println("4.同步通信完毕，得到消息内容：" + msgResponse);
    }

    private static void defaultFutureTest() {
        //1.开启消费线程 模拟B端消费消息
        Thread bConsumerThread = new Thread(() -> {
            while (true) {

                //获取消息
                String msgId = null;
                try {
                    msgId = mqttTopicB.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //消费..
                System.out.println("2." + Thread.currentThread().getName() + "消费消息：" + msgId);

                //消费完响应
                mqttTopicA.offer(msgId);

            }
        }, "B端消费线程");
        bConsumerThread.setDaemon(true);
        bConsumerThread.start();

        //2.开启接收线程 模拟A端接收消息
        Thread aReceivedThread = new Thread(() -> {
            while (true) {

                //获取消息
                String msgId = null;
                try {
                    msgId = mqttTopicA.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //唤醒业务处理线程
                MsgResponse msgResponse = new MsgResponse();
                msgResponse.setMsgId(msgId);
                //实际上消息通信会传输消息内容的，这里图方便 不想解析消息 就用msgID当作消息内容传输
                msgResponse.setMsgBodyJson("JSON消息内容");
                if (DefaultFuture.received(msgResponse)) {
                    System.out.println("3." + Thread.currentThread().getName() + "唤醒消息：" + msgId);
                }
            }
        }, "A端接收消息线程");
        aReceivedThread.setDaemon(true);
        aReceivedThread.start();


        //3.业务处理线程发送消息
        String msgId = UUID.randomUUID().toString();
        System.out.println("1.A端业务线程开始发送消息");
        MsgResponse msgResponse = sendMsg(msgId);
        System.out.println("4.同步通信完毕，得到消息内容：" + msgResponse);
    }


    private static MsgResponse sendMsg(String msgId) {
        DefaultFuture future = new DefaultFuture(msgId);
        //模拟MQTT通信 发送消息给B端 让其消费消息
        mqttTopicB.offer(msgId);
        MsgResponse msgResponse;
        try {
            msgResponse = future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return msgResponse;
    }


    private static MsgResponse sendMsgNoLock(String msgId) {
        DefaultFutureNoLock future = new DefaultFutureNoLock(msgId);
        //模拟MQTT通信 发送消息给B端 让其消费消息
        mqttTopicB.offer(msgId);
        MsgResponse msgResponse;
        try {
            msgResponse = future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return msgResponse;
    }
}
