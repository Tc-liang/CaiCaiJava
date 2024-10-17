package com.caicai.springbootrocketmq.consumer;

import org.springframework.stereotype.Component;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/9/12 10:17
 * @description:
 */
@Component
@ConsumerListener(topic = "TransactionTopicTest", consumerGroupName = "transaction_consumer_group")
public class TransactionConsumer {
}
