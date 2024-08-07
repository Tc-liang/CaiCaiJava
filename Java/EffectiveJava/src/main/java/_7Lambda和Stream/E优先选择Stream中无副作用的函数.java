package _7Lambda和Stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/29 15:55
 * @description:
 */
public class E优先选择Stream中无副作用的函数 {
    public static void main(String[] args) throws FileNotFoundException {
        List<String> list = Arrays.asList("aaa", "b", "cc");

        List<Integer> lengths = list.stream()
                .map(String::length)
                .sorted()
                .collect(Collectors.toList());

        System.out.println(lengths);
        System.out.println(list);
    }
}
