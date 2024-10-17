package com.caicai.springbootrocketmq.product.transaction;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/10/11 15:44
 * @description:
 */
public class OrderPayTransactionListener implements TransactionListener {
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object orderId) {
        try {
            //修改订单状态为已支付
            if (updatePayStatus((Long) orderId)) {
                return LocalTransactionState.COMMIT_MESSAGE;
            }
        } catch (Exception e) {
            //log
            return LocalTransactionState.UNKNOW;
        }
        return LocalTransactionState.ROLLBACK_MESSAGE;
    }


    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        Long orderId = Long.valueOf(msg.getBuyerId());
        //查询订单状态是否为已支付
        try {
            if (isPayed(orderId)) {
                return LocalTransactionState.COMMIT_MESSAGE;
            }
        } catch (Exception e) {
            //log
            return LocalTransactionState.UNKNOW;
        }

        return LocalTransactionState.ROLLBACK_MESSAGE;
    }

    private boolean isPayed(Long orderId) {
        return (orderId & 1) == 0;
    }


    private boolean updatePayStatus(Long orderId) {
        return (orderId & 1) == 1;
    }
}
