package F_Collections;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: Caicai
 * @Date: 2023-09-11 19:28
 * @Description:
 */
public class CollectionsTest {

    @Test
    public void testCopyOnWriteArrayList() throws InterruptedException {
//       1. List list  = new ArrayList();
//       2. List<String> list = new Vector<>();
//       3. List<String> list = Collections.synchronizedList(new ArrayList<>());
//       4. List<String> list = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<String> list =  new CopyOnWriteArrayList();

        for (int i = 0; i < 10; i++) {
            new Thread(()-> {
                list.add(UUID.randomUUID().toString().substring(0,5));
                System.out.println(list);
            },String.valueOf(i)).start();
        }

        TimeUnit.SECONDS.sleep(3);
    }

    @Test
    public void testConcurrentLinkedQueue()  {
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

        queue.offer("h1");
        queue.offer("h2");
        queue.offer("h3");

        String p1 = queue.poll();
        String p2 = queue.poll();
        String p3 = queue.poll();
        String p4 = queue.poll();

        queue.offer("h4");
        System.out.println(queue);
    }

    @Test
    public void test(){
        AtomicReference<String> reference = new AtomicReference<>();

        reference = null;
        System.out.println(reference.compareAndSet(null, null));
    }
}
