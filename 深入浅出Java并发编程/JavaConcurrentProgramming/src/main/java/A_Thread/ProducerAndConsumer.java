package A_Thread;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * ████        █████        ████████     █████ █████
 * ░░███      ███░░░███     ███░░░░███   ░░███ ░░███
 * ░███     ███   ░░███   ░░░    ░███    ░███  ░███ █
 * ░███    ░███    ░███      ███████     ░███████████
 * ░███    ░███    ░███     ███░░░░      ░░░░░░░███░█
 * ░███    ░░███   ███     ███      █          ░███░
 * █████    ░░░█████░     ░██████████          █████
 * ░░░░░       ░░░░░░      ░░░░░░░░░░          ░░░░░
 * <p>
 * 大吉大利            永无BUG
 *
 * @author 菜菜的后端私房菜
 * @Date 2022/2/26
 * @Description: 生产者消费者模型
 */
public class ProducerAndConsumer {
    static class Message {
        private int num;

        public Message(int num) {
            this.num = num;
        }

        @Override
        public String toString() {
            return num + "号消息";
        }
    }

    static class Producer {
        private Queue<Message> queue;
        private Object LOCK;


        public Producer(Queue<Message> queue, Object LOCK) {
            this.queue = queue;
            this.LOCK = LOCK;
        }

        public void produce(int num) throws InterruptedException {
            synchronized (LOCK) {
                while (queue.size() == 10) {
                    System.out.println("队列满了，生产者等待");
                    LOCK.wait();
                }
                Message message = new Message(num);
                System.out.println(Thread.currentThread().getName() + "生产了" + message);
                queue.add(message);
                LOCK.notifyAll();
            }
        }
    }

    static class Consumer {
        private Queue<Message> queue;
        private Object LOCK;

        public Consumer(Queue<Message> queue, Object LOCK) {
            this.queue = queue;
            this.LOCK = LOCK;
        }

        public void consume() throws InterruptedException {
            synchronized (LOCK) {
                while (queue.isEmpty()) {
                    System.out.println("队列空了，消费者等待");
                    LOCK.wait();
                }
                Message message = queue.poll();
                System.out.println(Thread.currentThread().getName() + "消费了" + message);
                LOCK.notifyAll();
            }
        }
    }


    public static void main(String[] args) {
        ArrayBlockingQueue<Message> queue = new ArrayBlockingQueue<>(10);
        Object LOCK = new Object();

        Producer producer = new Producer(queue, LOCK);
        Consumer consumer = new Consumer(queue, LOCK);

        new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    producer.produce(i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "生产者1号").start();
        new Thread(() -> {
            for (int i = 100; i < 200; i++) {
                try {
                    producer.produce(i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "生产者2号").start();

        new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    consumer.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "消费者1号").start();
        new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    consumer.consume();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "消费者2号").start();
    }

}
