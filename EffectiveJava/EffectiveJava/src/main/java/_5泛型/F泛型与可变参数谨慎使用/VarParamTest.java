package _5泛型.F泛型与可变参数谨慎使用;

import java.util.Arrays;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/26 22:59
 * @description:
 */
public class VarParamTest {
    public static void main(String[] args) {
        List<String> strings = Arrays.asList("123", "456");
        dangerous(strings);
    }

    static void dangerous(List<String>... stringLists) {
        List<Integer> intList = Arrays.asList(42);
        Object[] objects = stringLists;
        // Heap pollution
        objects[0] = intList;
        // 报错ClassCastException
        String s = stringLists[0].get(0);
    }
}
