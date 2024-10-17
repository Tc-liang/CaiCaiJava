package com.caicai.springbootrocketmq.controller;

import com.caicai.springbootrocketmq.product.ServerProduct;
import com.caicai.springbootrocketmq.product.ServerTransactionProduct;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.selector.SelectMessageQueueByHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;


@RequestMapping("/warn")
@RestController
@Slf4j
public class WarnController {

    private static final String topic = "TopicTest";
    private static final String transactionTopic = "TransactionTopicTest";

    @Autowired
    private ServerProduct producer;

    @Autowired
    private ServerTransactionProduct transactionProduct;

    @GetMapping("/syncSend")
    public SendResult syncSend() {
        return producer.sendSyncMsg(topic, "tag", "sync hello world!");
    }

    @GetMapping("/syncSendSelect/{id}")
    public SendResult syncSendSelect(@PathVariable Long id) {
        return producer.sendSyncMsgSelector(topic, "tag", "sync hello world!", new SelectMessageQueueByHash(), id.toString());
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

    @GetMapping("/sendOnewayMsg/{count}")
    public String onewaySend(@PathVariable int count) {
        byte[] payload = generateThreeMBString();
        for (int i = 0; i < count; i++) {
            producer.sendOnewayMsg(topic, "*", payload);
        }
        return "sendOnewayMsg ok";
    }

    @GetMapping("/sendKeyMsg/{count}")
    public String sendKeyMsg(@PathVariable int count) {
        String key = "CAICAIJAVA";
        for (int i = 0; i < count; i++) {
            producer.sendOnewayMsg(topic, "*", key.getBytes(StandardCharsets.UTF_8));
        }
        return "sendOnewayMsg ok";
    }


    @GetMapping("/sendDelayMsg/{delayLevel}")
    public String sendDelayMsg(@PathVariable("delayLevel") int delayLevel) {
        String msg = "DelayMsg";
        producer.sendDelayMsg(topic, "*", msg.getBytes(StandardCharsets.UTF_8), delayLevel);
        log.info("发送延时消息成功");
        return "sendDelayMsg ok";
    }

    @GetMapping("/sendOrderMsg")
    public String sendOrderMsg() {
        String msg = "sendOrderMsg1";
        producer.sendOrderMsg(topic, msg, new SelectMessageQueueByHash(), "1");
        return "sendOrderMsg ok";
    }

    @GetMapping("/sendTransactionMsg/{orderId}")
    public String sendTransactionMsg(@PathVariable("orderId") Integer orderId) {
        String msg = "sendTransactionMsg " + orderId;
        transactionProduct.sendTransactionMsg(transactionTopic, msg, orderId.toString());
        return "send " + msg + "ok";
    }

    private static byte[] generateThreeMBString() {
        // 3MB的字节数
        int threeMB = 3 * 1024 * 1024;

        // 生成3MB大小的字符串
        Random random = new Random();
        StringBuilder sb = new StringBuilder(threeMB);
        for (int i = 0; i < threeMB; i++) {
            char c = (char) ('A' + random.nextInt(26)); // 生成随机大写字母
            sb.append(c);
        }

        // 将字符串转换为字节数组
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}


