package A_内存管理篇.A2_Java内存区域与内存溢出.oom;

/**
 * @author Tc.l
 * @Date 2020/10/27
 * @Description: 测试栈溢出StackOverflowError
 * -Xss128k
 * 没有递归终止条件
 */
public class StackOverFlowError {
    private int depth = 1;

    public void recursion() {
        depth++;
        recursion();
    }

    public static void main(String[] args) throws Throwable {
        StackOverFlowError sof = new StackOverFlowError();
        try {
            sof.recursion();
        } catch (Throwable e) {
            System.out.println("depth:" + sof.depth);
            throw e;
        }
    }
}
