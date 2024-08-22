package com.caicai.springbootrocketmq.product;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.nio.charset.StandardCharsets;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/21 15:46
 * @description:
 */
public class ServerProduct {

    private DefaultMQProducer producer;

    public ServerProduct(String producerGroup) {
        producer = new DefaultMQProducer(producerGroup);
        init();
    }

    public ServerProduct() {
        producer = new DefaultMQProducer("Default_Server_Producer_Group");
        init();
    }

    private void init() {
        producer.setNamesrvAddr("127.0.0.1:9876");
        //set...

        try {
            producer.start();
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }

    public SendResult sendSyncMsg(String topic, String tag, String jsonBody) {
        Message message = new Message(topic, tag, jsonBody.getBytes(StandardCharsets.UTF_8));
        SendResult sendResult;
        try {
            sendResult = producer.send(message);
        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return sendResult;
    }

    public void sendAsyncMsg(String topic, String tag, String jsonBody, SendCallback sendCallback) {
        Message message = new Message(topic, tag, jsonBody.getBytes(StandardCharsets.UTF_8));
        try {
            producer.send(message, sendCallback);
        } catch (MQClientException | RemotingException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendOnewayMsg(String topic, String tag, String jsonBody) {
        Message message = new Message(topic, tag, jsonBody.getBytes(StandardCharsets.UTF_8));
        try {
            producer.sendOneway(message);
        } catch (MQClientException | RemotingException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
