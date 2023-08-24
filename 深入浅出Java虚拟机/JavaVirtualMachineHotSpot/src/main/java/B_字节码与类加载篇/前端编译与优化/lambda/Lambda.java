package B_字节码与类加载篇.前端编译与优化.lambda;

/**
 * @author Tcl
 * @Date 2021/5/24
 * @Description:
 */
public class Lambda {
    private int i = 10;

    public static void main(String[] args) {
        test(() -> System.out.println("匿名内部类实现函数式接口"));
    }

    public static void test(LambdaTest lambdaTest) {
        lambdaTest.lambda();
    }
}

@FunctionalInterface
interface LambdaTest {
    void lambda();
}

