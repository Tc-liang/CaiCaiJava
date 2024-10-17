package com.caicai.springbootrocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/22 17:35
 * @description:
 */
@Component
@Slf4j
public class ConsumerListenerAnnotationHandler {

    @Autowired
    private ThreadPoolTaskExecutor executor;

    private ApplicationContext applicationContext;

    @Autowired
    public ConsumerListenerAnnotationHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void handler() throws MQClientException {
        Map<String, Object> map = applicationContext.getBeansWithAnnotation(ConsumerListener.class);
        if (CollectionUtils.isEmpty(map)) {
            return;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object annotationInstance = entry.getValue();
            ConsumerListener consumerListener = annotationInstance.getClass().getAnnotation(ConsumerListener.class);
            String groupName = consumerListener.consumerGroupName();
            String topic = consumerListener.topic();
            String tag = consumerListener.tag();


            //并发推送
            concurrentPushStart(groupName, topic, tag);

            //顺序推送
//            orderPushStart(groupName, topic, tag);

            //轻量级拉取
//            litePullStart(groupName, topic, tag);
        }
    }

    private static void orderPushStart(String groupName, String topic, String tag) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
//        consumer.setMessageListener((MessageListenerOrderly) (msgs, context) -> {
//            System.out.printf("%s Receive  Messages: %s %n", Thread.currentThread().getName(), msgs);
//            return ConsumeOrderlyStatus.SUCCESS;
//        });

        consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
            try {
                for (MessageExt msg : msgs) {
                    // 获取消息的重试次数
                    int retryCount = msg.getReconsumeTimes();
                    System.out.println("Message [" + msg.getMsgId() + "] is reconsumed " + retryCount + " times");

                    //如果重试次数超过阈值 记录
                    if (retryCount >= 3) {
                        System.out.println("Message [" + msg.getMsgId() + "] add DB");
                    }

                    // 模拟消费失败
                    if (retryCount < 3) {
                        throw new RuntimeException("Consume failed");
                    }

                    // 消费成功
                    System.out.println("Message [" + msg.getMsgId() + "] consumed successfully");
                }
                return ConsumeOrderlyStatus.SUCCESS;
            } catch (Exception e) {
                // 记录日志
                e.printStackTrace();
                // 返回重试状态
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        });

        consumer.setConsumerGroup(groupName);
        consumer.subscribe(topic, tag);
        //根据配置文件set...
        consumer.setNamesrvAddr("127.0.0.1:9876");
        consumer.start();
    }

    private static void concurrentPushStart(String groupName, String topic, String tag) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
        consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            MessageExt messageExt = msgs.get(0);
            log.info("{}Receive  MessagesID:{} orderId:{}", Thread.currentThread().getName(), messageExt.getMsgId(), messageExt.getBuyerId());
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
//                log.error("模拟重试 消费失败 ,msgId:{}", msgs.get(0).getMsgId());
//                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        });
        consumer.setConsumerGroup(groupName);
        consumer.subscribe(topic, tag);
        //根据配置文件set...
        consumer.setNamesrvAddr("127.0.0.1:9876");
        consumer.start();
    }

    private void litePullStart(String groupName, String topic, String tag) throws MQClientException {
        DefaultLitePullConsumer consumer = new DefaultLitePullConsumer();
        //使用注解中设置的值
        consumer.setConsumerGroup(groupName);
        consumer.subscribe(topic, tag);
        //根据配置文件set...
        consumer.setNamesrvAddr("127.0.0.1:9876");
        consumer.start();
        executor.execute(() -> {
            while (true) {
                List<MessageExt> poll = consumer.poll();
                log.info("{}拉取消息:{}", groupName, poll);
            }
        });
    }
}
