package C_垃圾回收篇.引用;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tc.l
 * @Date 2020/11/19
 * @Description:
 * 弱引用测试
 */
public class SoftReferenceTest {
    public static void main(String[] args) {
        int[] list = new int[10];
        SoftReference listSoftReference = new SoftReference(list);
        list = null;
//      以上三行代码等价下面这行使用匿名对象的代码
//      SoftReference listSoftReference = new SoftReference(new int[10]);

        //[I@61bbe9ba
        System.out.println(listSoftReference.get());

        //模拟空间资源不足
        try{
            byte[] bytes = new byte[1024 * 1024 * 4];
            System.gc();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //null
            System.out.println(listSoftReference.get());
        }

    }
}
