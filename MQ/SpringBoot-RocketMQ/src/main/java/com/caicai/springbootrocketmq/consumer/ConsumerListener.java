package com.caicai.springbootrocketmq.consumer;

import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/22 17:33
 * @description:
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsumerListener {

    String topic();

    String tag() default "*";

    String consumerGroupName() default "default_consumer_group";

}
