package _7Lambda和Stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/29 14:39
 * @description:
 */
public class C坚持使用标准函数接口 {
    public static void main(String[] args) {
        List<Integer> numbers = new ArrayList<>();
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);
        numbers.add(4);
        numbers.add(5);

        // 创建一个谓词，检查数字是否是偶数
        Predicate<Integer> isEven = n -> n % 2 == 0;

        // 使用谓词过滤列表
        numbers.stream()
                .filter(isEven)
                .forEach(System.out::println);



        // 创建一个Supplier，每次调用get方法都会生成一个新的随机数
        Supplier<Integer> randomIntSupplier = () -> (int) (Math.random() * 100);

        // 获取并打印5个随机数
        for (int i = 0; i < 5; i++) {
            System.out.println(randomIntSupplier.get());
        }




        List<String> greetings = Arrays.asList("Hello", "World", "!");

        // 创建一个consumer，用于打印接收到的消息
        Consumer<String> printer = message -> System.out.println(message);

        // 对列表中的每个元素应用consumer
        greetings.forEach(printer);



        List<String> names = Arrays.asList("John Doe", "Jane Smith", "Alice Johnson");

        // 创建一个Function，将人名转换为姓氏
        Function<String, String> getLastName = name -> name.split(" ")[1];

        // 将所有名字转换为姓氏
        List<String> lastNames = names.stream()
                .map(getLastName)
                .collect(Collectors.toList());

        // 输出：[Doe, Smith, Johnson]
        System.out.println(lastNames);
    }
}
