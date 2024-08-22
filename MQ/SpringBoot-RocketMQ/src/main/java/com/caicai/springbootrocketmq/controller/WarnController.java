package com.caicai.springbootrocketmq.controller;

import com.caicai.springbootrocketmq.product.ServerProduct;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/warn")
@RestController
@Slf4j
public class WarnController {

    private static final String topic = "TopicTest";

    @Autowired
    private ServerProduct producer;

    @GetMapping("/syncSend")
    public SendResult syncSend() {
        return producer.sendSyncMsg(topic, "tag", "sync hello world!");
    }

    @GetMapping("/asyncSend")
    public String asyncSend() {
        producer.sendAsyncMsg(topic, "tag", "async hello world!", new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("消息发送成功{}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("消息发送失败", throwable);
                //记录后续重试
            }
        });
        return "asyncSend ok";
    }

    @GetMapping("/sendOnewayMsg")
    public String onewaySend() {
        producer.sendOnewayMsg(topic, "tag", "oneway hello world!");
        return "sendOnewayMsg ok";
    }
}
