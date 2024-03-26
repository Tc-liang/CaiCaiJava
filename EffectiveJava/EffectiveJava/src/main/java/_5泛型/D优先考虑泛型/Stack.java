package _5泛型.D优先考虑泛型;

import java.util.ArrayList;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/26 22:44
 * @description:
 */
public class Stack<E> {
    //1.定义泛型数组 实例化使用Object数组强转
    private E[] elements;
    private int size = 0;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    @SuppressWarnings("unchecked")
    public Stack() {
        elements = (E[]) new Object[DEFAULT_INITIAL_CAPACITY];
    }


    public static void main(String[] args) {
        //2.使用Object数组，读取元素时强转 ArrayList
        ArrayList<String> list = new ArrayList<>();
        list.add("菜菜的后端私房菜");
        list.get(0);
    }

}

