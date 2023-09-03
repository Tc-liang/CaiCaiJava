package C_AQSComponent;

import org.junit.Test;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author: Caicai
 * @Date: 2023-09-02 19:59
 * @Description:
 */
public class B_ComponentTest {

    @Test
    public void testReentrantLock(){
        ReentrantLock reentrantLock = new ReentrantLock();
        reentrantLock.lock();
        try{
            //....
        }finally {
            reentrantLock.unlock();
        }
    }


    @Test
    public void testReentrantReadWriteLock(){
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
        ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
        ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

        readLock.lock();
        readLock.unlock();

        writeLock.lock();
        writeLock.unlock();
    }





}
