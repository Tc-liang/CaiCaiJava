package _9通用编程;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/2 10:33
 * @description:
 */
public class E基本数据类型优于包装类 {
    public static void main(String[] args) {

//        Comparator<Integer> naturalOrder = (i, j) -> (i < j) ? -1 : (i == j ? 0 : 1);
//        int result1 = naturalOrder.compare(new Integer(42), new Integer(42));
//        // 1
//        System.out.println(result1);


        Comparator<Integer> naturalOrder = (iBoxed, jBoxed) -> {
            //自动拆箱
            int i = iBoxed, j = jBoxed;
            return i < j ? -1 : (i == j ? 0 : 1);
        };
        int result2 = naturalOrder.compare(new Integer(42), new Integer(42));
        // 0
        System.out.println(result2);


        //性能问题
        Long sum = 0L;
        for (long i = 0; i < Integer.MAX_VALUE; i++) {
            sum += i;
            System.out.println(sum);
        }

        
    }
}