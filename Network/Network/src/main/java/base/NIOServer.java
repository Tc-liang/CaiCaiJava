package base;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/5/11 17:16
 * @description:
 */
public class NIOServer {
    public static void main(String[] args) throws IOException {
        //服务端通道
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(8080));
        //设置通道非阻塞
        serverChannel.configureBlocking(false);

        //服务端注册 监听accept事件
        Selector selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT, null);


        //循环处理
        while (true) {
            //阻塞直到事件发生
            selector.select();

            //事件发生 轮询处理
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                if (key.isAcceptable()){
                    //如果是请求连接的事件 说明 客户端开始连接
                    ServerSocketChannel sc = (ServerSocketChannel) key.channel();
                    SocketChannel clientChannel = sc.accept();

                    //设置非阻塞
                    clientChannel.configureBlocking(false);

                    //将客户端通道注册到select 监听读事件 附件为缓冲区 因为后续要通过缓冲区读数据
                    clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(5));
                }
            }

        }
    }
}
