package _6枚举和注解.F注解优于命名模式;

import java.lang.annotation.*;

// Marker annotation type declaration - Page 180
import java.lang.annotation.*;


/**
 * 定义注解
 * 只在无参静态方法上使用
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {
}