package _7Lambda和Stream;

import java.math.BigInteger;
import java.util.stream.LongStream;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/29 16:24
 * @description:
 */
public class G谨慎使用Stream并行 {
    static long piParallel(long n) {
        return LongStream.rangeClosed(2, n)
                .parallel()
                .mapToObj(BigInteger::valueOf)
                .filter(i -> i.isProbablePrime(50))
                .count();
    }

    static long pi(long n) {
        return LongStream.rangeClosed(2, n)
                .mapToObj(BigInteger::valueOf)
                .filter(i -> i.isProbablePrime(50))
                .count();
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        piParallel(10_000_000);
        // 3079
        System.out.println(System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        pi(10_000_000);
        // 17194
        System.out.println(System.currentTimeMillis() - start);
    }
}
