package reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

public class PerformanceTest {

    public static void main(String[] args) throws Throwable {
        // 初始化 MethodHandle
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType type = MethodType.methodType(String.class, String.class);
        MethodHandle methodHandle = lookup.findStatic(TestClass.class, "testMethod", type);

        // 初始化反射 Method
        Method method = TestClass.class.getMethod("testMethod", String.class);

        // 定义测试次数
        int testCount = 100_000_000;

        // MethodHandle 性能测试
        Instant start = Instant.now();
        for (int i = 0; i < testCount; i++) {
            String msg = (String) methodHandle.invokeExact("test");
        }
        Instant end = Instant.now();
        Duration methodHandleDuration = Duration.between(start, end);
        System.out.println("MethodHandle took: " + methodHandleDuration.toMillis() + " ms");

        // 反射性能测试
        start = Instant.now();
        for (int i = 0; i < testCount; i++) {
            method.invoke(null, "test");
        }
        end = Instant.now();
        Duration reflectionDuration = Duration.between(start, end);
        System.out.println("Reflection took: " + reflectionDuration.toMillis() + " ms");
    }
}