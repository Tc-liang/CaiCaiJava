package _5泛型.E有限通配符;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/26 22:48
 * @description:
 */
public class LimitTest {
    public static void main(String[] args) {

        // 上限通配符
        List<? extends Number> numbers = new ArrayList<>();
        //无法写
//        numbers.add(1);

        numbers = Arrays.asList(1,2,3);
        //只能读
        numbers.forEach(System.out::println);



        // 下限通配符
        List<? super Number> superNumbers = new ArrayList<>();
        superNumbers.add(new Integer(123));
        superNumbers.add(new Long(123));
        superNumbers.add(new BigDecimal("123.33"));
        //只能读到Object类型
        for (Object o : superNumbers) {
            System.out.println(o);
        }

    }


}
