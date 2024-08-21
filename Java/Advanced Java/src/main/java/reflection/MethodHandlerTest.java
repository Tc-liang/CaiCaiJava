package reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/8/15 9:29
 * @description:
 */
public class MethodHandlerTest {
    public static void main(String[] args) throws Throwable {

        //MethodHandle初始化
        MethodHandle methodHandlerConstructor = MethodHandles
                .lookup()
                .findConstructor(ReflectionObject.class, MethodType.methodType(void.class));
        MethodHandle setName = MethodHandles
                .lookup()
                .findVirtual(ReflectionObject.class, "setName", MethodType.methodType(void.class, String.class));
        MethodHandle setAge = MethodHandles
                .lookup()
                .findVirtual(ReflectionObject.class, "setAge", MethodType.methodType(void.class, int.class));


        //反射初始化
        Class<ReflectionObject> objectClass = ReflectionObject.class;
        Constructor<ReflectionObject> reflectionConstructor = objectClass.getConstructor();
        Method setNameMethod = objectClass.getDeclaredMethod("setName", String.class);
        Field field = objectClass.getDeclaredField("age");
        field.setAccessible(true);

        int[] loopCounts = {1, 1_000, 1_000_000, 1_000_000_000};
        for (int index = 0; index < loopCounts.length; index++) {
            long startTime = System.currentTimeMillis();
            for (int inner = 0; inner < loopCounts[index]; inner++) {
                methodHandler(methodHandlerConstructor, setName, setAge);
//                reflection(reflectionConstructor, setNameMethod, field);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("循环次数：" + loopCounts[index] + "，耗时：" + (endTime - startTime) + "ms");
        }
    }

    /**
     * 循环次数：1，耗时：1ms
     * 循环次数：1000，耗时：2ms
     * 循环次数：1000000，耗时：15ms
     * 循环次数：1000000000，耗时：5875ms
     * @param reflectionConstructor
     * @param setNameMethod
     * @param field
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private static void reflection(Constructor<ReflectionObject> reflectionConstructor, Method setNameMethod, Field field) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        ReflectionObject instance = reflectionConstructor.newInstance();
        setNameMethod.invoke(instance, "菜菜的后端私房菜");
        field.set(instance, 18);
    }

    /**
     * 循环次数：1，耗时：1ms
     * 循环次数：1000，耗时：2ms
     * 循环次数：1000000，耗时：15ms
     * 循环次数：1000000000，耗时：5875ms
     * @param constructor
     * @param setName
     * @param setAge
     * @throws Throwable
     */
    private static void methodHandler(MethodHandle constructor, MethodHandle setName, MethodHandle setAge) throws Throwable {
        ReflectionObject reflectionObject = (ReflectionObject) constructor.invoke();
        setName.invoke(reflectionObject, "菜菜的后端私房菜");
        setAge.invoke(reflectionObject, 18);
    }


}
