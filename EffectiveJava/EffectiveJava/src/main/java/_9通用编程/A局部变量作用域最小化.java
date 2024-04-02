package _9通用编程;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/2 8:31
 * @description:
 */
public class A局部变量作用域最小化 {
    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);

        //for循环局部变量作用域只在循环中
        for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
            System.out.println(iterator.next());
        }

        for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
            System.out.println(iterator.next());
        }

        //while循环作用域更大
        Iterator<Integer> i1 = list.iterator();
        while (i1.hasNext()) {
            System.out.println(i1.next());
        }

        Iterator<Integer> i2 = list.iterator();
        //CV大法忘记修改 i1
        while (i1.hasNext()) {
            System.out.println(i1.next());
        }

    }
}
