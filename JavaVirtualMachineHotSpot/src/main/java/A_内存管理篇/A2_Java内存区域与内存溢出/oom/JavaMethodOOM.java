package A_内存管理篇.A2_Java内存区域与内存溢出.oom;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author Tc.l
 * @Date 2020/10/27
 * @Description: cglib创建对象导致元空间满 频繁full gc
 * -XX:MaxMetaspaceSize=10m
 * -XX:MetaspaceSize=10m
 */
public class JavaMethodOOM {
    public static void main(String[] args) {
        while (true) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(JavaMethodOOM.class);
            enhancer.setUseCache(false);
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                    System.out.println("create" + obj);
                    return proxy.invokeSuper(obj, args);
                }
            });
            enhancer.create();
        }
    }

    static class ObjectOOM {

    }
}
