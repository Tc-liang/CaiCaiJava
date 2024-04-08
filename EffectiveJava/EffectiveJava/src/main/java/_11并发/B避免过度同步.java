package _11并发;

import java.util.*;

/**
 * @author: cl
 * @create: 2024/4/8 10:56
 * @description:
 */
public class B避免过度同步 {
    public static void main(String[] args) {
        Collection<Integer> collection = Collections.synchronizedCollection(Arrays.asList(1, 2, 3));
        List<Integer> vector = new Vector<>();
    }
}
