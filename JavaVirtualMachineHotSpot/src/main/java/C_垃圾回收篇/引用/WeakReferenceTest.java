package C_垃圾回收篇.引用;


import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Tc.l
 * @Date 2020/11/19
 * @Description: 软引用测试
 */
public class WeakReferenceTest {
    public static void main(String[] args) {
        test2();
    }

    public static void test1() {
        WeakReference<int[]> weakReference = new WeakReference<>(new int[1]);
        //[I@511d50c0
        System.out.println(weakReference.get());

        System.gc();
        //null
        System.out.println(weakReference.get());
    }

    public static void test2() {
        WeakHashMap<String, String> weakHashMap = new WeakHashMap<>();
        HashMap<String, String> hashMap = new HashMap<>();

        String s1 = new String("3.jpg");
        String s2 = new String("4.jpg");

        hashMap.put(s1, "图片1");
        hashMap.put(s2, "图片2");
        weakHashMap.put(s1, "图片1");
        weakHashMap.put(s2, "图片2");

        //只将s1赋值为空时,那个堆中的3.jpg字符串还会存在强引用,所以要remove
        hashMap.remove(s1);
        s1=null;
        s2=null;

        System.gc();

        System.out.println("hashMap:");
        test2Iteration(hashMap);

        System.out.println("weakHashMap:");
        test2Iteration(weakHashMap);
    }

    private static void test2Iteration(Map<String, String>  map){
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()){
           Map.Entry entry = (Map.Entry) iterator.next();
            System.out.println(entry);
        }
    }
}
