package _5泛型.A不要使用原生态类型;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/24 14:04
 * @description:
 */
public class TypeTest {

    public static void main(String[] args) {
//        origin();
//        object();

        List<?> list = new ArrayList<>();
        //编译报错
//        list.add("123");
//        list.add(456);


        //合法
        Class<List> listClass = List.class;

        //不合法
//        List<Object>.class

        List<Object> arrayList = new ArrayList<>();
        //合法
        if (arrayList instanceof List){
            List<?> lists = arrayList;
        }

        //不合法
//        if (arrayList instanceof List<Object>){
//
//        }
    }

    private static void extracted() {

    }

    private static void object() {
        List<Object> list = new ArrayList<>();
        list.add("123");
        list.add(456);
        list.add(new int[]{});

        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            System.out.println(next);
        }
    }

    private static List origin() {
        //原生态泛型
        List list = new ArrayList();
        //加入时不会报错
        list.add("123");
        list.add(456);


        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            //读数据 强转时报错
            String next = (String) iterator.next();
            System.out.println(next);
        }
        return list;
    }
}
