package _6枚举和注解.F注解优于命名模式;

public class Sample {

    //满足无参静态能通过
    @Test
    public static void m1() {
    }

    //抛出异常 不能通过
    @Test
    public static void m2() {
        throw new RuntimeException("Boom");
    }

    //不能通过 不是静态
    @Test
    public void m3() {
    }

    // Test should fail
    @Test
    public static void m4() {
        throw new RuntimeException("Crash");
    }

}