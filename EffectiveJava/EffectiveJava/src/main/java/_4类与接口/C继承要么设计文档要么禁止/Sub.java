package _4类与接口.C继承要么设计文档要么禁止;

/**
 * @author: 菜菜的后端私房菜aiCai
 * @create: 2024/3/21 15:42
 * @description:
 */
public class Sub extends Super {
    private String msg;

    public Sub(String msg) {
        this.msg = msg;
    }

    @Override
    protected void method() {
        System.out.println("sub msg -> " + msg);
    }

    public static void main(String[] args) {
        //sub msg -> null
        Sub sub = new Sub("ok");
        //sub msg -> ok
        sub.method();
    }
}
