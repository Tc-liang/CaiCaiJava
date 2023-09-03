package C_AQSComponent;

import java.util.concurrent.Exchanger;

/**
 * @Author: Caicai
 * @Date: 2023-09-03 19:41
 * @Description:
 */
public class F_Exchanger {
    public static void main(String[] args) {
        Exchanger<String> exchanger = new Exchanger();

        new Thread(() -> {
            String A = "A";
            try {
                //B
                System.out.println(exchanger.exchange(A));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        String B = "B";
        try {
            //A
            String A = exchanger.exchange(B);
            System.out.println("A=" + A + " B=" + B);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
