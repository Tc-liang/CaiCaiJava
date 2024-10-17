package com.caicai.springbootrocketmq.product;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/10/11 11:36
 * @description:
 */
public class ServerTransactionProduct {
    private TransactionMQProducer transactionProducer;

    private TransactionListener transactionListener;

    public ServerTransactionProduct(TransactionListener transactionListener) {
        this.transactionProducer = new TransactionMQProducer("Default_Server_Transaction_Producer_Group");
        this.transactionListener = transactionListener;
        init();
    }

    private void init() {
        transactionProducer.setNamesrvAddr("127.0.0.1:9876");
        //set...
        transactionProducer.setTransactionListener(transactionListener);

        try {
            transactionProducer.start();
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }


    public void sendTransactionMsg(String topic, String body, String orderId) {
        Message msg = new Message();
        msg.setBody(body.getBytes());
        msg.setBuyerId(orderId);
        msg.setTopic(topic);
        try {
            transactionProducer.sendMessageInTransaction(msg, orderId);
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }
}
