package B_字节码与类加载篇.虚拟机字节码执行引擎;

/**
 * @author Tc.l
 * @Date 2020/11/11
 * @Description:
 * 动态分派
 */
public class DynamicDispatch {
    public static void main(String[] args) {
        Father son = new Son();
        Father father = new Father();

        son.hello(new Nod());
        father.hello(new Wave());
    }
    static class Father{
        public void hello(Nod nod){
            System.out.println("Father nod hello " );
        }

        public void hello(Wave wave){
            System.out.println("Father wave hello " );
        }
    }

    static class Son extends Father{

        @Override
        public void hello(Nod nod) {
            System.out.println("Son nod hello");
        }

        @Override
        public void hello(Wave wave) {
            System.out.println("Son wave hello");
        }
    }

    //招手
    static class Wave{}
    //点头
    static class Nod{}
}


