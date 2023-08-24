package D_性能调优篇.GUI工具的使用;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author Tcl
 * @Date 2021/5/21
 * @Description:
 * -Xmx600m -Xms600m -XX:SurvivorRatio=8
 */
public class JProfileTest {
    private String name;
    private int age;
    private byte[] bytes;


    public JProfileTest(String name, int age, byte[] bytes) {
        this.name = name;
        this.age = age;
        this.bytes = bytes;
    }

    public static void main(String[] args) {
        ArrayList<JProfileTest> list = new ArrayList<>();
        int l = 0;
        while (true){
            list.add(new JProfileTest("name"+l,l++,new byte[1024 * 10]));
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
