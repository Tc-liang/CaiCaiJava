package B_字节码与类加载篇.虚拟机字节码执行引擎;

/**
 * @author Tcl
 * @Date 2021/4/26
 * @Description:
 */
public class Son extends Father{

    public Son() {
        //invokespecial 调用父类init 非虚方法
        super();
        //invokestatic 调用父类静态方法 非虚方法
        staticMethod();
        //invokespecial 调用子类私有方法 特殊的非虚方法
        privateMethod();
        //invokevirtual 调用子类的重写方法 虚方法
        overrideMethod();
        //invokespecial 调用父类方法 非虚方法
        super.overrideMethod();
        //invokespecial 调用父类final方法 非虚方法
        super.finalMethod();
        //invokedynamic 动态生成接口的实现类 动态调用
        TestInterfaceMethod test = ()->{
            System.out.println("testInterfaceMethod");
        };
        //invokeinterface 调用接口方法 虚方法
        test.testInterfaceMethod();
    }

    @Override
    public void overrideMethod(){
        System.out.println("son override method");
    }

    private void privateMethod(){
        System.out.println("son private method");
    }

    public static void main(String[] args) {
        new Son();
    }
}
