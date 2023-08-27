package B_synchronized;

import java.util.concurrent.locks.Condition;

/**
 * @Author: Caicai
 * @Date: 2023-08-25 22:16
 * @Description:synchronized字节码指令实现
 */
public class A_SynchronizedByteCode {
    public static void main(String[] args) {
        Object obj = new Object();
        synchronized (obj) {

        }


    }

    public static synchronized void test() {

    }
}
