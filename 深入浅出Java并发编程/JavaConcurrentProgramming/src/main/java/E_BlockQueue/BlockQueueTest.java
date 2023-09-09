package E_BlockQueue;

import org.junit.Test;

import java.util.concurrent.*;

/**
 * @Author: Caicai
 * @Date: 2023-09-08 22:19
 * @Description:
 */
public class BlockQueueTest {

    @Test
    public void testArrayBlockingQueue() {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue(3);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);

        //队列: 1 2 3
        print(queue);

        //false
        System.out.println(queue.offer(4));
        Integer poll = null;
        try {
            poll = queue.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //1 出队
        System.out.println(poll + " 出队");

        try {
            queue.put(4);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //队列: 2 3 4
        print(queue);
    }

    @Test
    public void testLinkedBlockingQueue() {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(3);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        queue.offer(4);
        //队列: 1 2 3
        print(queue);

        //1
        System.out.println(queue.poll());
        //队列: 2 3
        print(queue);

        queue.offer(5);
        //队列: 2 3 5
        print(queue);
    }


    @Test
    public void testLinkedBlockingDeque() {
        LinkedBlockingDeque<Integer> queue = new LinkedBlockingDeque<>(3);
        queue.offerLast(1);
        queue.offerLast(2);
        queue.offerLast(3);
        queue.offerLast(4);
        //队列: 1 2 3
        print(queue);
        //1
        System.out.println(queue.pollFirst());
        //队列: 2 3
        print(queue);

        queue.offerLast(5);
        //队列: 2 3 5
        print(queue);
    }


    @Test
    public void testPriorityBlockingQeque() {
        PriorityBlockingQueue<Integer> queue = new PriorityBlockingQueue<>(6);
        queue.offer(99);
        queue.offer(1099);
        queue.offer(299);
        queue.offer(992);
        queue.offer(99288);
        queue.offer(995);
        //99 299 992 995 1099 99288
        while (!queue.isEmpty()){
            System.out.print(" "+queue.poll());
        }

        System.out.println();

        queue = new PriorityBlockingQueue<>(6, (o1, o2) -> o2-o1);
        queue.offer(99);
        queue.offer(1099);
        queue.offer(299);
        queue.offer(992);
        queue.offer(99288);
        queue.offer(995);
        //99288 1099 995 992 299 99
        while (!queue.isEmpty()){
            System.out.print(" "+queue.poll());
        }
    }


    @Test
    public void testDelayBlockingQeque() throws InterruptedException {
        DelayQueue queue = new DelayQueue();
    }


    @Test
    public void testSynchronousQueue() throws InterruptedException {
        final SynchronousQueue<Integer> queue = new SynchronousQueue(true);
        new Thread(() -> {
            try {
                queue.put(1);
                queue.put(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "put12线程").start();

        new Thread(() -> {
            try {
                queue.put(3);
                queue.put(4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "put34线程").start();

        TimeUnit.SECONDS.sleep(1);
        System.out.println(Thread.currentThread().getName() + "拿出" + queue.take());
        TimeUnit.SECONDS.sleep(1);
        System.out.println(Thread.currentThread().getName() + "拿出" + queue.take());
        TimeUnit.SECONDS.sleep(1);
        System.out.println(Thread.currentThread().getName() + "拿出" + queue.take());
        TimeUnit.SECONDS.sleep(1);
        System.out.println(Thread.currentThread().getName() + "拿出" + queue.take());
    }

    @Test
    public void testTransfer() throws InterruptedException {
        LinkedTransferQueue queue = new LinkedTransferQueue();
        new Thread(()->{
            try {
                //阻塞直到被获取
                queue.transfer(1);
                System.out.println(Thread.currentThread().getName()+"放入的1被取走了");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"生产者").start();

        TimeUnit.SECONDS.sleep(3);
        System.out.println(Thread.currentThread().getName()+"取出队列中的元素");
        queue.poll();
    }

    @Test
    public void testTryTransfer() throws InterruptedException {
        LinkedTransferQueue<Integer> queue = new LinkedTransferQueue<>();
        //false
        System.out.println(queue.tryTransfer(1));
        //null
        System.out.println(queue.poll());

        new Thread(()->{
            try {
                //消费者取出2
                System.out.println(Thread.currentThread().getName()+"取出"+queue.poll(2, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        },"消费者").start();
        TimeUnit.SECONDS.sleep(1);
        //true
        System.out.println(queue.tryTransfer(2));
    }


    private <T> void print(BlockingQueue<T> queue) {
        System.out.print("队列: ");
        for (T t : queue) {
            System.out.print(t + " ");
        }
        System.out.println();
    }

}
