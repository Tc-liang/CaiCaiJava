package _10异常;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/4/3 8:18
 * @description:
 */
public class A针对异常情况才使用异常 {
    public static void main(String[] args) {
        int[] ints = {1, 2, 3, 4, 5};
        int index = 0;
        try {
            while (true) {
                System.out.println(ints[index++]);
            }
        } catch (ArrayIndexOutOfBoundsException e) {

        }

    }
}
