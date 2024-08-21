package com.caicai.springbootrocketmq.product;

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
    public ServerProduct producer() {
        return new ServerProduct();
    }
}
