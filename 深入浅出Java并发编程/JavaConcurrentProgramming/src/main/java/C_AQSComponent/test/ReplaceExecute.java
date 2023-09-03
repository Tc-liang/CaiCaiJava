package C_AQSComponent.test;

import C_AQSComponent.A_MySynchronizedComponent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @Author: Caicai
 * @Date: 2023-09-02 19:10
 * @Description:交替执行
 */
public class ReplaceExecute {
    int code = 1;

    public static void main(String[] args) {
        Lock lock = new A_MySynchronizedComponent();
        Condition conA = lock.newCondition();
        Condition conB = lock.newCondition();
        Condition conC = lock.newCondition();
        ReplaceExecute replaceExecute = new ReplaceExecute();

        Thread threadA = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                printA(lock, conA, conB, replaceExecute);
            }
        });

        Thread threadB = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                printB(lock, conB, conC, replaceExecute);
            }
        });


        Thread threadC = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                printC(lock, conA, conC, replaceExecute);
            }
        });

        threadA.start();
        threadB.start();
        threadC.start();

        try {
            threadA.join();
            threadB.join();
            threadC.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private static void printC(Lock lock, Condition conA, Condition conC, ReplaceExecute replaceExecute) {
        lock.lock();
        try {
            try {
                while (replaceExecute.code != 3) {
                    conC.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("C");
            replaceExecute.code = 1;
            conA.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private static void printB(Lock lock, Condition conB, Condition conC, ReplaceExecute replaceExecute) {
        lock.lock();
        try {
            try {
                while (replaceExecute.code != 2) {
                    conB.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("B");
            replaceExecute.code = 3;
            conC.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private static void printA(Lock lock, Condition conA, Condition conB, ReplaceExecute replaceExecute) {
        lock.lock();
        try {
            try {
                while (replaceExecute.code != 1) {
                    conA.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("A");
            replaceExecute.code = 2;
            conB.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
