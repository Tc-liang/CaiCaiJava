package com.caicai.springbootrocketmq.consumer;

import org.springframework.stereotype.Component;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/9/10 11:19
 * @description:
 */
@Component
@ConsumerListener(topic = "TopicTestOrder", consumerGroupName = "order_consumer_group")
public class OrderConsumer {
}
