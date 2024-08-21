package com.caicai.springboottcpserver.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/16 9:19
 * @description:
 */
@Slf4j
public class NettyServer {

    public void init() {
        int bossThreadCount = 1;
        int workThreadCount = 10;
        int port = 1111;

        NioEventLoopGroup bossGroup = new NioEventLoopGroup(bossThreadCount);
        NioEventLoopGroup workGroup = new NioEventLoopGroup(workThreadCount);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        socketChannel.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(128, 9, 1, 5, 0))
//                                .addLast(new LoggingHandler(LogLevel.INFO))
                                //烟感设备TCP消息编解码
                                .addLast(new com.caicai.springboottcpserver.netty.SmokeDeviceDecoder())
                                //业务处理 发送到MQ
                                .addLast(new com.caicai.springboottcpserver.netty.SendMQHandler());
                    }
                });

        ChannelFuture channelFuture = serverBootstrap.bind(port);
        try {
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("netty服务关闭阻塞被中断", e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            log.info("netty资源关闭结束");
        }

    }

    public static void main(String[] args) {
        int bossThreadCount = 1;
        int workThreadCount = 10;
        int port = 1111;

        NioEventLoopGroup bossGroup = new NioEventLoopGroup(bossThreadCount);
        NioEventLoopGroup workGroup = new NioEventLoopGroup(workThreadCount);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        socketChannel.pipeline()
                                .addLast(new LengthFieldBasedFrameDecoder(128, 9, 1, 5, 0))
                                .addLast(new LoggingHandler(LogLevel.INFO))
                                //烟感设备TCP消息编解码
                                .addLast(new com.caicai.springboottcpserver.netty.SmokeDeviceDecoder())
                                //业务处理 发送到MQ
                                .addLast(new com.caicai.springboottcpserver.netty.SendMQHandler());
                    }
                });

        try {
            ChannelFuture serverChanelFuture = serverBootstrap.bind(port).sync();
            serverChanelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
