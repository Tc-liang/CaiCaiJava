package A_Thread;

import java.util.concurrent.TimeUnit;

/**
 * @author Tc.l
 * @Date 2020/12/4
 * @Description: 打标记 + 中断标识符
 */
public class MarkAndInterrupt {
    boolean flag = true;
    int i ;
    public static void main(String[] args) {
        MarkAndInterrupt mai = new MarkAndInterrupt();

        Thread thread = new Thread(() -> {
            while (mai.flag && !Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("i=" + mai.i++);
                    //等待操作
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {

                    //因为等待时 中断线程会再抛出异常前恢复标志位, 所以重新 改变中断标志
                    Thread.currentThread().interrupt();
                    System.out.println(Thread.currentThread().getName()+"发生中断异常");
                }
            }
            System.out.println("标记:" + mai.flag + " , 中断标识位:" + Thread.currentThread().isInterrupted());
        });

        thread.start();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //终止操作
        mai.flag=true;
        //中断thread后,因为thread在等待状态,它的中断状态会被清除,并抛出中断异常,我在catch中继续对它进行中断,所以它的标志位会变成true从而退出循环,达到终止线程的效果
        thread.interrupt();
    }
}
