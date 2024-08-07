package _7Lambda和Stream;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/29 15:36
 * @description:
 */
public class D谨慎使用Stream {
    public static void main(String[] args) {
        //3375633756303402151831471311692515133756
        "菜菜的后端私房菜".chars().forEach(System.out::print);
        System.out.println();
        "菜菜的后端私房菜".chars().forEach(x -> System.out.print((char) x));
    }
}
