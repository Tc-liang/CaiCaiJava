package base;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/5/11 17:08
 * @description:
 */
public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 8080);
             OutputStream outputStream = socket.getOutputStream()) {
            outputStream.write("download~".getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
