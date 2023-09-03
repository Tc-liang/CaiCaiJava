package C_AQSComponent.test.proAndCon;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author: Caicai
 * @Date: 2023-09-02 19:18
 * @Description:
 */
public class Consumer {
    Lock lock;
    Condition proCondition;
    Condition conCondition;

    public Consumer(Lock lock, Condition proCondition, Condition conCondition) {
        this.lock = lock;
        this.proCondition = proCondition;
        this.conCondition = conCondition;
    }

    public void con(Num num) {
        lock.lock();
        try {
            while (num.getFood() == 0) {
                conCondition.await();
            }
            System.out.println(Thread.currentThread().getName() + "消费食物，剩余：" + num.subFood());
            proCondition.signalAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
