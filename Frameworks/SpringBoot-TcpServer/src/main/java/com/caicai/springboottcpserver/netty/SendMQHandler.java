package com.caicai.springboottcpserver.netty;

import com.alibaba.fastjson.JSON;
import com.caicai.springboottcpserver.tcp.entity.BaseTcpDTO;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/16 11:41
 * @description: 解析TCP成JSON后发送到MQ
 */
@Slf4j
public class SendMQHandler extends SimpleChannelInboundHandler<BaseTcpDTO> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, BaseTcpDTO baseTcpDTO) {
        String json = JSON.toJSONString(baseTcpDTO);
        log.info(json);

        channelHandlerContext
                .writeAndFlush(baseTcpDTO)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("响应写回失败 error:", future.cause());
                    }
                });

    }
}
