package com.caicai.springbootrocketmq.consumer;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/21 16:32
 * @description:
 */
@Component
@RocketMQMessageListener(topic = "TopicTest", consumerGroup = "warn_consumer_group")
public class WarnConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        // 处理消息
        System.out.println("Received message: " + message);
    }
}