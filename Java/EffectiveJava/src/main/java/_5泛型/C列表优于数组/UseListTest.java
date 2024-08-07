package _5泛型.C列表优于数组;

import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/26 22:41
 * @description:
 */
public class UseListTest {
    public static void main(String[] args) {
        Object[] objects = new Long[2];
        //运行时 ArrayStoreException
        objects[0] = "1233123";

        //允许
        List<?>[] lists2 = new List<?>[2];
        //报错 创建泛型数组
//        List<Integer>[] lists = new List<Integer>[5];
    }
}
