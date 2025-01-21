import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/12/25 16:08
 * @description:
 */
public class Server {
    public static void main(String[] args) {
        NioEventLoopGroup parentGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup childGroup = new NioEventLoopGroup(10);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        ChannelFuture future = serverBootstrap
                .group(parentGroup, childGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        channel.pipeline()
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        System.out.println("in 1 start");
                                        ctx.fireChannelRead(msg);
                                        System.out.println("in 1 end");
                                    }
                                })
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        System.out.println("in 2 start");
                                        System.out.println(msg);

                                        ctx.channel().writeAndFlush("hello ~ my name is cai cai !");
//                                        ctx.writeAndFlush("hello ~ my name is cai cai !");

                                        System.out.println("in 2 end");
                                    }
                                })
                                .addLast(new ChannelOutboundHandlerAdapter() {
                                    @Override
                                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                        System.out.println("out 1 start:" + msg);
                                        ByteBuf byteBuf = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(msg.toString()), StandardCharsets.UTF_8);
                                        ctx.writeAndFlush(byteBuf);
                                        System.out.println("out 1 end:" + msg);
                                    }
                                })
                                .addLast(new ChannelOutboundHandlerAdapter() {
                                    @Override
                                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                                        System.out.println("out 2 start:" + msg);
                                        ctx.write(msg);
                                        System.out.println("out 2 end:" + msg);
                                    }
                                })
                        ;
                    }
                })
                .bind(8888);
        try {
            future.sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }
}
