import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/12/25 16:21
 * @description:
 */
public class Client {
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        ChannelFuture future = bootstrap
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new StringDecoder())
                                .addLast(new StringEncoder())
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        System.out.println("客户端收到：" + msg);
                                    }
                                })
                        ;
                    }
                })
                .connect("127.0.0.1", 8888);

        try {
            future.sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        future.channel().writeAndFlush("hello cai cai ~");
    }
}
