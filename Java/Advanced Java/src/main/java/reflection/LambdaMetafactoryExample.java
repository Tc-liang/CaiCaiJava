package reflection;

import java.lang.invoke.*;
/**
 * @author: 菜菜的后端私房菜
 * @create: 2024/12/11 11:04
 * @description:
 */
public class LambdaMetafactoryExample {
    public static void sayHello(String name) {
        System.out.println("Hello, " + name);
    }

    public static void main(String[] args) throws Throwable {
        // 获取 Lookup 对象
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        // 编写方法类型 返回、入参类型
        MethodType methodType = MethodType.methodType(void.class, String.class);
        // 查找目标方法
        MethodHandle targetMethod = lookup.findStatic(LambdaMetafactoryExample.class, "sayHello", methodType);

        // 准备元数据并创建 CallSite
        CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "greet", // lambda 方法名
            MethodType.methodType(Greeter.class), // 调用点签名
            methodType, // 目标方法类型（注意这里应该直接是 methodType）
            targetMethod, // 目标方法句柄
            methodType // 目标方法类型
        );

        // 获取并调用工厂方法
        MethodHandle factory = callSite.getTarget();
        Greeter greeter = (Greeter) factory.invokeWithArguments();

        // 调用 lambda 表达式
        greeter.greet("World");
    }
}