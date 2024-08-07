package C_垃圾回收篇.GC;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/4
 * @Description:-Xms10m -Xmx10m -XX:+PrintGCDetails
 */
public class OOMTest {
    public static void main(String[] args) {
//        while (true){
//            byte[] bytes = new byte[1000 * 1024 * 1024];
//        }
        for (int i = 0; i < 10; i++) {
            byte[] bytes = new byte[1000 * 1024 * 1024];
        }
    }
}
