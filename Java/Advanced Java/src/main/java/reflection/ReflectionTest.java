package reflection;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Scanner;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/8 10:03
 * @description: -Xms250m -Xmx250m
 */
public class ReflectionTest {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String numStr = "";

        while (!numStr.equals("exit")) {
            numStr = scanner.nextLine();
            int count = Integer.parseInt(numStr);
            long start = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                jdkReflection();
//                jdkConstructorReflection();
//                springReflection();
//                springConstructorReflection();
            }
            System.out.println(System.currentTimeMillis() - start);
        }
    }



    /**
     * 耗时
     * 1
     * 1
     * 10
     * 0
     * 100
     * 0
     * 1000
     * 2
     * 10000
     * 4
     * 100000
     * 3
     * 1000000
     * 17
     * 10000000
     * 76
     */
    private static void jdkConstructorReflection() {
        Class<ReflectionObject> objectClass = ReflectionObject.class;
        try {
            Constructor<ReflectionObject> constructor = objectClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 耗时
     * 1
     * 42
     * 10
     * 0
     * 100
     * 0
     * 1000
     * 1
     * 10000
     * 3
     * 100000
     * 4
     * 1000000
     * 44
     * 10000000
     * 251
     */
    private static void springConstructorReflection() {
        Constructor<ReflectionObject> constructor = null;
        try {
            constructor = ReflectionUtils.accessibleConstructor(ReflectionObject.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 耗时
     * 1
     * 2
     * 10
     * 0
     * 100
     * 2
     * 1000
     * 4
     * 10000
     * 12
     * 100000
     * 16
     * 1000000
     * 285
     * 10000000
     * 3198
     */
    private static void jdkReflection() {
        Class<ReflectionObject> objectClass = ReflectionObject.class;
        try {
            Constructor<ReflectionObject> constructor = objectClass.getConstructor();
            ReflectionObject instance = constructor.newInstance();

            Method setNameMethod = objectClass.getDeclaredMethod("setName", String.class);
            setNameMethod.invoke(instance, "菜菜的后端私房菜");

            Field field = objectClass.getDeclaredField("age");
            field.setAccessible(true);
            field.set(instance, 18);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException |
                 NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 耗时
     * 1
     * 49
     * 10
     * 5
     * 100
     * 3
     * 1000
     * 4
     * 10000
     * 8
     * 100000
     * 7
     * 1000000
     * 74
     * 10000000
     * 494
     */
    private static void springReflection() {
        Constructor<ReflectionObject> constructor = null;
        ReflectionObject instance = null;
        try {
            constructor = ReflectionUtils.accessibleConstructor(ReflectionObject.class);
            instance = constructor.newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Method setNameMethod = ReflectionUtils.findMethod(ReflectionObject.class, "setName", String.class);
        if (Objects.nonNull(setNameMethod)) {
            ReflectionUtils.invokeMethod(setNameMethod, instance, "菜菜的后端私房菜");
        }

        Field field = ReflectionUtils.findField(ReflectionObject.class, "age");
        if (Objects.nonNull(field)) {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, instance, 18);
        }
    }
}
