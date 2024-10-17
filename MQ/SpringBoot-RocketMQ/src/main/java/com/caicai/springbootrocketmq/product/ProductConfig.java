package com.caicai.springbootrocketmq.product;

import com.caicai.springbootrocketmq.product.transaction.TransactionListenerImpl;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/21 15:41
 * @description:
 */
@Configuration
public class ProductConfig {

    @Bean
    public ServerProduct serverProduct() {
        return new ServerProduct();
    }

    @Bean
    public ServerTransactionProduct serverTransactionProduct() {
        return new ServerTransactionProduct(transactionListener());
    }

    @Bean
    public TransactionListener transactionListener() {
        return new TransactionListenerImpl();
    }
}
