package com.caicai.springbootrocketmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageQueueListener;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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


//            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
            DefaultLitePullConsumer consumer = new DefaultLitePullConsumer();

            //使用注解中设置的值
            consumer.setConsumerGroup(groupName);
            consumer.subscribe(topic, tag);

//            consumer.setMessageListener((MessageListenerConcurrently) (msgs, context) -> {
//                System.out.printf("%s Receive  MessagesID: %s %n", Thread.currentThread().getName(), msgs.get(0).getMsgId());
//                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
////                log.error("模拟重试 消费失败 ,msgId:{}", msgs.get(0).getMsgId());
////                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
//            });

//            consumer.setMessageListener((MessageListenerOrderly) (msgs, context) -> {
//                System.out.printf("%s Receive  Messages: %s %n", Thread.currentThread().getName(), msgs);
//                return ConsumeOrderlyStatus.SUCCESS;
//            });

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
}
