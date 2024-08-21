package com.caicai.springboottcpserver.netty;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/16 9:19
 * @description:
 */
@Configuration
public class NettyConfig {
    @Bean
    public NettyServer nettyServer(){
        NettyServer nettyServer = new NettyServer();
        nettyServer.init();
        return nettyServer;
    }
}
