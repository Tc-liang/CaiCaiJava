package _7Lambda和Stream;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/29 13:50
 * @description:
 */
public class ALambda优先于匿名类 {
    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        list.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });

        list.sort((o1, o2) -> o2.compareTo(o1));
        list.sort(Comparator.reverseOrder());

        System.out.println(list);


    }
}
