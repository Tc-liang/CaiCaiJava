package com.caicai.springbootrocketmq.consumer;

import org.springframework.stereotype.Component;

@Component
@ConsumerListener(topic = "TopicTest", consumerGroupName = "warn_consumer_group")
public class WarnConsumer {
}