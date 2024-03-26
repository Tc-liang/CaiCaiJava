package _4类与接口.D为后代设计接口;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/21 17:24
 * @description:
 */
public class InterfaceDefaultMethod {
    public static void main(String[] args) {
        List<Integer> numbers = new ArrayList<>();
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);
        numbers.add(4);
        numbers.add(5);

        // 使用lambda表达式定义断言：元素是否为偶数
        numbers.removeIf(n -> n % 2 == 0);

        // 输出：[1, 3, 5]
        System.out.println(numbers);
    }
}
