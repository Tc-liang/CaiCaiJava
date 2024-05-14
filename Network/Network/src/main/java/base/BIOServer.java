package base;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/24 8:41
 * @description: BIO案例
 */
public class BIOServer {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(8080);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            //阻塞获取客户端连接
            try (Socket socket = serverSocket.accept();
                 OutputStream outputStream = socket.getOutputStream()) {
                //模拟读取磁盘数据写回网卡
                outputStream.write("hello".getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
