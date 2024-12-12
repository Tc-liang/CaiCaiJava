package reflection;


import java.lang.invoke.*;
import java.lang.reflect.Method;
/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/12/11 10:04
 * @description:
 */
public class LambdaVsReflectionBenchmark {

    // 目标方法
    public static void sayHello(String name) {
        name = "Hello," + name;
//        System.out.println("Hello, " + name);
    }

    // 使用 LambdaMetafactory 创建 lambda 表达式
    private static Greeter createLambda() throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType methodType = MethodType.methodType(void.class, String.class);
        MethodHandle targetMethod = lookup.findStatic(LambdaVsReflectionBenchmark.class, "sayHello", methodType);

        CallSite callSite = LambdaMetafactory.metafactory(
                lookup,
                "greet",
                MethodType.methodType(Greeter.class),
                methodType.changeReturnType(void.class),
                targetMethod,
                methodType
        );

        MethodHandle factory = callSite.getTarget();
        return (Greeter) factory.invokeExact();
    }

    // 使用反射获取目标方法
    private static Method getReflectiveMethod() throws Exception {
        return LambdaVsReflectionBenchmark.class.getMethod("sayHello", String.class);
    }

    // 测试调用次数
//    private static final long ITERATIONS = 1_000L;
    private static final long ITERATIONS = 1_000_000_000L;

    /**
     * 调用次数:1000
     * 直接调用耗时: 0.13 ms
     * LambdaMetafactory 调用耗时: 0.17 ms
     * 反射调用耗时: 1.74 ms
     * <p>
     * 调用次数:1000000000
     * 直接调用耗时: 4501.54 ms
     * LambdaMetafactory 调用耗时: 4640.59 ms
     * 反射调用耗时: 6142.39 ms
     *
     * @param args
     * @throws Throwable
     */
    public static void main(String[] args) throws Throwable {

        System.out.println("调用次数:" + ITERATIONS);
        //直接调用
        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sayHello("World");
        }
        long end = System.nanoTime();
        System.out.printf("直接调用耗时: %.2f ms%n", (end - start) / 1e6);

        // 准备 Lambda
        Greeter lambdaGreeter = createLambda();

        // 测试 Lambda 调用
        long lambdaStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            lambdaGreeter.greet("World");
        }
        long lambdaEnd = System.nanoTime();
        System.out.printf("LambdaMetafactory 调用耗时: %.2f ms%n", (lambdaEnd - lambdaStart) / 1e6);


        // 准备反射方法
        Method reflectiveMethod = getReflectiveMethod();

        // 测试反射调用
        long reflectionStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            reflectiveMethod.invoke(null, "World");
        }
        long reflectionEnd = System.nanoTime();
        System.out.printf("反射调用耗时: %.2f ms%n", (reflectionEnd - reflectionStart) / 1e6);
    }
}