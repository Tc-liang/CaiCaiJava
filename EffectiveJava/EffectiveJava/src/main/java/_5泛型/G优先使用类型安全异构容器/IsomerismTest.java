package _5泛型.G优先使用类型安全异构容器;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/3/26 23:01
 * @description:
 */
public class IsomerismTest {

    private Map<Class<?>, Object> map = new HashMap<>();

    public <T> void put(Class<T> type, T instance) {
        map.put(Objects.requireNonNull(type), instance);
    }

    public <T> T get(Class<T> type) {
        return type.cast(map.get(type));
    }

    public static void main(String[] args) {
        IsomerismTest f = new IsomerismTest();
        f.put(String.class, "Java");
        f.put(Class.class, IsomerismTest.class);
        f.put(Double[].class, new Double[]{1.1, 2.2});

        //Java
        String string = f.get(String.class);
        System.out.println(string);

        //IsomerismTest
        Class<?> cClass = f.get(Class.class);
        System.out.println(cClass.getSimpleName());

        //1.1
        //2.2
        Double[] doubles = f.get(Double[].class);
        for (Double aDouble : doubles) {
            System.out.println(aDouble);
        }
    }
}
