package C_AQSComponent.test.proAndCon;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class Product {
    Lock lock;
    Condition proCondition;
    Condition conCondition;

    public Product(Lock lock, Condition proCondition, Condition conCondition) {
        this.lock = lock;
        this.proCondition = proCondition;
        this.conCondition = conCondition;
    }

    public void pro(Num num) {
        lock.lock();
        try {
            while (num.getFood() == num.max()) {
                proCondition.await();
            }
            System.out.println(Thread.currentThread().getName() + "生产食物，剩余：" + num.addFood());
            conCondition.signalAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
