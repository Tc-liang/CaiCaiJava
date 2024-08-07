package D_性能调优篇.内存泄漏;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author 菜菜的后端私房菜
 * @Date 2021/5/20
 * @Description:
 */
public class CacheTest {
    private static Map<String, String> weakHashMap = new WeakHashMap<>();
    private static  Map<String, String> map = new HashMap<>();
    public static void main(String[] args) {
        //模拟要缓存的对象
        String s1 = new String("O1");
        String s2 = new String("O2");
        weakHashMap.put(s1,"S1");
        map.put(s2,"S2");

        //模拟不再使用缓存
        s1=null;
        s2=null;

        //垃圾回收WeakHashMap中存的弱引用
        System.gc();
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //遍历各个散列表
        System.out.println("============HashMap===========");
        traverseMaps(map);
        System.out.println();
        System.out.println("============WeakHashMap===========");
        traverseMaps(weakHashMap);
    }

    private static void traverseMaps(Map<String, String> map){
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println(entry);
        }
    }
}
