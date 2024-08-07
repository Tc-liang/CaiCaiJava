package _9通用编程;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/2 9:24
 * @description:
 */
public class Bforeach优于for循环 {
    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        //for循环
        for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
            System.out.println(iterator.next());
        }
        for (int index = 0; index < list.size(); index++) {
            System.out.println(list.get(index));
        }

        //foreach
        for (Integer integer : list) {
            System.out.println(integer);
        }
    }
}
